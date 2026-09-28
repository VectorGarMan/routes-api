package routesservices.routesservices.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import routesservices.routesservices.dto.RouteResponseDto;
import routesservices.routesservices.entity.DeliveryPoint;
import routesservices.routesservices.entity.Route;
import routesservices.routesservices.entity.RouteStop;
import routesservices.routesservices.repository.RouteRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** BE-013: marcar parada visitada respeta el orden y es idempotente. */
@ExtendWith(MockitoExtension.class)
class RouteStopServiceTest {

    @Mock
    private RouteRepository routeRepository;

    private RouteStopService service() {
        return new RouteStopService(routeRepository);
    }

    @Test
    void marcaLaPrimeraParadaPendienteComoVisitada() {
        DeliveryPoint pointA = point("A");
        DeliveryPoint pointB = point("B");
        Route route = buildRoute(pointA, pointB);
        mockFindAndSave(route);

        RouteResponseDto result = service().markVisited(route.getId(), pointA.getId());

        assertThat(result.stops().get(0).status()).isEqualTo("VISITED");
        assertThat(result.stops().get(1).status()).isEqualTo("PENDING");
        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    @Test
    void rechazaSaltarUnaParadaPendienteAnterior() {
        DeliveryPoint pointA = point("A");
        DeliveryPoint pointB = point("B");
        Route route = buildRoute(pointA, pointB);
        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));

        assertThatThrownBy(() -> service().markVisited(route.getId(), pointB.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void marcarUnaParadaYaVisitadaEsIdempotente() {
        DeliveryPoint pointA = point("A");
        DeliveryPoint pointB = point("B");
        Route route = buildRoute(pointA, pointB);
        route.getStops().get(0).setStatus("VISITED");
        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));

        RouteResponseDto result = service().markVisited(route.getId(), pointA.getId());

        assertThat(result.stops().get(0).status()).isEqualTo("VISITED");
    }

    @Test
    void marcarLaUltimaParadaPendienteCompletaLaRuta() {
        DeliveryPoint pointA = point("A");
        DeliveryPoint pointB = point("B");
        Route route = buildRoute(pointA, pointB);
        route.getStops().get(0).setStatus("VISITED");
        mockFindAndSave(route);

        RouteResponseDto result = service().markVisited(route.getId(), pointB.getId());

        assertThat(result.status()).isEqualTo("COMPLETED");
    }

    private void mockFindAndSave(Route route) {
        when(routeRepository.findById(route.getId())).thenReturn(Optional.of(route));
        when(routeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Route buildRoute(DeliveryPoint pointA, DeliveryPoint pointB) {
        Route route = Route.builder()
                .id(UUID.randomUUID())
                .requestId(UUID.randomUUID())
                .status("ACTIVE")
                .objective("TIME")
                .depotPointId(pointA.getId())
                .totalDistanceMeters(BigDecimal.valueOf(1000))
                .totalTimeSeconds(BigDecimal.valueOf(200))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .stops(new ArrayList<>())
                .build();

        route.getStops().add(RouteStop.builder().route(route).point(pointA).stopOrder(0).status("PENDING").build());
        route.getStops().add(RouteStop.builder().route(route).point(pointB).stopOrder(1).status("PENDING").build());
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
