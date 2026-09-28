package routesservices.routesservices.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import routesservices.routesservices.client.DistanceTimeResult;
import routesservices.routesservices.client.OptimizeResponsePayload;
import routesservices.routesservices.client.PythonOptimizerClient;
import routesservices.routesservices.dto.DistanceMatrixResult;
import routesservices.routesservices.dto.RouteResponseDto;
import routesservices.routesservices.entity.DeliveryPoint;
import routesservices.routesservices.entity.Route;
import routesservices.routesservices.entity.RouteStop;
import routesservices.routesservices.repository.RouteRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * BE-011: umbral de cambio significativo = 25% de diferencia relativa por tramo.
 * Cubre los 3 casos del done_when (sin cambio, cambio bajo, cambio significativo)
 * y la invariante de que las paradas visitadas nunca reaparecen como pendientes.
 */
@ExtendWith(MockitoExtension.class)
class RouteRecalculationServiceTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private PointDistanceService pointDistanceService;
    @Mock
    private DistanceMatrixService distanceMatrixService;
    @Mock
    private PythonOptimizerClient pythonOptimizerClient;

    private RouteRecalculationService service() {
        return new RouteRecalculationService(routeRepository, pointDistanceService, distanceMatrixService, pythonOptimizerClient);
    }

    @Test
    void sinCambioNoRecalcula() {
        Route route = buildRouteWithOneVisitedAndTwoPending(BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        mockFindById(route);

        when(pointDistanceService.getDistanceAndTime(any(), any(), any()))
                .thenReturn(new DistanceTimeResult(BigDecimal.valueOf(1000), BigDecimal.valueOf(100)));

        RouteResponseDto result = service().recalculateIfNeeded(route.getId());

        assertThat(result.stops()).hasSize(3);
        verifyNoInteractions(pythonOptimizerClient);
    }

    @Test
    void cambioBajoNoRecalcula() {
        Route route = buildRouteWithOneVisitedAndTwoPending(BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        mockFindById(route);

        // 10% de diferencia: por debajo del umbral de 25%.
        when(pointDistanceService.getDistanceAndTime(any(), any(), any()))
                .thenReturn(new DistanceTimeResult(BigDecimal.valueOf(1000), BigDecimal.valueOf(110)));

        service().recalculateIfNeeded(route.getId());

        verifyNoInteractions(pythonOptimizerClient);
    }

    @Test
    void cambioSignificativoRecalculaYExcluyeVisitadas() {
        DeliveryPoint pointA = point("A"); // visitado
        DeliveryPoint pointB = point("B");
        DeliveryPoint pointC = point("C");
        Route route = buildRoute(pointA, pointB, pointC, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        mockFindById(route);

        // 100% de diferencia en el primer tramo pendiente: dispara el recálculo.
        when(pointDistanceService.getDistanceAndTime(any(), any(), any()))
                .thenReturn(new DistanceTimeResult(BigDecimal.valueOf(1000), BigDecimal.valueOf(200)));

        BigDecimal[][] distanceMatrix = {{BigDecimal.ZERO, BigDecimal.valueOf(500), BigDecimal.valueOf(700)},
                {BigDecimal.valueOf(500), BigDecimal.ZERO, BigDecimal.valueOf(300)},
                {BigDecimal.valueOf(700), BigDecimal.valueOf(300), BigDecimal.ZERO}};
        BigDecimal[][] timeMatrix = {{BigDecimal.ZERO, BigDecimal.valueOf(50), BigDecimal.valueOf(70)},
                {BigDecimal.valueOf(50), BigDecimal.ZERO, BigDecimal.valueOf(30)},
                {BigDecimal.valueOf(70), BigDecimal.valueOf(30), BigDecimal.ZERO}};
        when(distanceMatrixService.buildMatrix(any(), any(), any()))
                .thenReturn(new DistanceMatrixResult(distanceMatrix, timeMatrix));

        // Python invierte el orden de los pendientes: A(depósito actual) -> C -> B.
        when(pythonOptimizerClient.optimize(any())).thenReturn(new OptimizeResponsePayload(
                UUID.randomUUID().toString(),
                List.of(pointA.getId().toString(), pointC.getId().toString(), pointB.getId().toString()),
                List.of(0, 2, 1),
                1200.0,
                100.0,
                "SUCCESS",
                null,
                null
        ));
        when(routeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RouteResponseDto result = service().recalculateIfNeeded(route.getId());

        verify(pythonOptimizerClient, times(1)).optimize(any());
        assertThat(result.stops()).hasSize(3);
        assertThat(result.stops().get(0).pointId()).isEqualTo(pointA.getId().toString());
        assertThat(result.stops().get(0).status()).isEqualTo("VISITED");
        assertThat(result.stops().get(1).pointId()).isEqualTo(pointC.getId().toString());
        assertThat(result.stops().get(1).status()).isEqualTo("PENDING");
        assertThat(result.stops().get(2).pointId()).isEqualTo(pointB.getId().toString());
        assertThat(result.stops().get(2).status()).isEqualTo("PENDING");
    }

    @Test
    void lasParadasVisitadasNuncaReaparecenComoPendientes() {
        DeliveryPoint pointA = point("A");
        DeliveryPoint pointB = point("B");
        DeliveryPoint pointC = point("C");
        Route route = buildRoute(pointA, pointB, pointC, BigDecimal.valueOf(100), BigDecimal.valueOf(100));
        mockFindById(route);

        when(pointDistanceService.getDistanceAndTime(any(), any(), any()))
                .thenReturn(new DistanceTimeResult(BigDecimal.valueOf(1000), BigDecimal.valueOf(200)));

        BigDecimal[][] matrix3x3 = {{BigDecimal.ZERO, BigDecimal.valueOf(500), BigDecimal.valueOf(700)},
                {BigDecimal.valueOf(500), BigDecimal.ZERO, BigDecimal.valueOf(300)},
                {BigDecimal.valueOf(700), BigDecimal.valueOf(300), BigDecimal.ZERO}};
        when(distanceMatrixService.buildMatrix(any(), any(), any()))
                .thenReturn(new DistanceMatrixResult(matrix3x3, matrix3x3));

        when(pythonOptimizerClient.optimize(any())).thenReturn(new OptimizeResponsePayload(
                UUID.randomUUID().toString(),
                List.of(pointA.getId().toString(), pointC.getId().toString(), pointB.getId().toString()),
                List.of(0, 2, 1),
                1200.0,
                100.0,
                "SUCCESS",
                null,
                null
        ));
        when(routeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().recalculateIfNeeded(route.getId());

        long pendientesConIdDeVisitada = route.getStops().stream()
                .filter(stop -> "PENDING".equals(stop.getStatus()) && stop.getPoint().getId().equals(pointA.getId()))
                .count();
        assertThat(pendientesConIdDeVisitada).isZero();
        assertThat(route.getStops().stream().filter(stop -> "VISITED".equals(stop.getStatus())).count()).isEqualTo(1);
    }

    private void mockFindById(Route route) {
        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
    }

    private Route buildRouteWithOneVisitedAndTwoPending(BigDecimal legBSeconds, BigDecimal legCSeconds) {
        return buildRoute(point("A"), point("B"), point("C"), legBSeconds, legCSeconds);
    }

    private Route buildRoute(DeliveryPoint pointA, DeliveryPoint pointB, DeliveryPoint pointC,
                              BigDecimal legBSeconds, BigDecimal legCSeconds) {
        Route route = Route.builder()
                .id(UUID.randomUUID())
                .requestId(UUID.randomUUID())
                .status("ACTIVE")
                .objective("TIME")
                .depotPointId(pointA.getId())
                .totalDistanceMeters(BigDecimal.valueOf(1500))
                .totalTimeSeconds(BigDecimal.valueOf(200))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .stops(new ArrayList<>())
                .build();

        route.getStops().add(RouteStop.builder()
                .route(route).point(pointA).stopOrder(0).status("VISITED")
                .estimatedLegSeconds(BigDecimal.ZERO).build());
        route.getStops().add(RouteStop.builder()
                .route(route).point(pointB).stopOrder(1).status("PENDING")
                .estimatedLegSeconds(legBSeconds).build());
        route.getStops().add(RouteStop.builder()
                .route(route).point(pointC).stopOrder(2).status("PENDING")
                .estimatedLegSeconds(legCSeconds).build());
        return route;
    }

    private DeliveryPoint point(String reference) {
        return DeliveryPoint.builder()
                .id(UUID.randomUUID())
                .reference(reference)
                .latitude(BigDecimal.valueOf(20.5))
                .longitude(BigDecimal.valueOf(-103.3))
                .build();
    }
}
