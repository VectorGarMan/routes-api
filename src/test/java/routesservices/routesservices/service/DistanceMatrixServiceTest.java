package routesservices.routesservices.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import routesservices.routesservices.client.DistanceTimeResult;
import routesservices.routesservices.dto.DistanceMatrixResult;
import routesservices.routesservices.entity.DeliveryPoint;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica que la matriz NxN respete points[i] -> points[j] para las 9 direcciones
 * posibles entre 3 puntos (incluyendo diagonal), sin BD ni Mapbox real (mock puro).
 */
@ExtendWith(MockitoExtension.class)
class DistanceMatrixServiceTest {

    @Mock
    private PointDistanceService pointDistanceService;

    @Test
    void construyeMatrizNxNRespetandoElOrdenYTodasLasDirecciones() {
        DeliveryPoint p0 = point(0);
        DeliveryPoint p1 = point(1);
        DeliveryPoint p2 = point(2);
        List<DeliveryPoint> points = List.of(p0, p1, p2);

        when(pointDistanceService.getDistanceAndTime(any(), any()))
                .thenAnswer(invocation -> {
                    DeliveryPoint origin = invocation.getArgument(0);
                    DeliveryPoint destination = invocation.getArgument(1);
                    int i = index(origin);
                    int j = index(destination);
                    return new DistanceTimeResult(
                            BigDecimal.valueOf(i * 10 + j),
                            BigDecimal.valueOf(i * 100 + j));
                });

        DistanceMatrixService service = new DistanceMatrixService(pointDistanceService);
        DistanceMatrixResult result = service.buildMatrix(points);

        assertThat(result.distanceMatrix()).hasDimensions(3, 3);
        assertThat(result.timeMatrix()).hasDimensions(3, 3);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertThat(result.distanceMatrix()[i][j])
                        .as("distanceMatrix[%d][%d] = points[%d] -> points[%d]", i, j, i, j)
                        .isEqualByComparingTo(BigDecimal.valueOf(i * 10 + j));
                assertThat(result.timeMatrix()[i][j])
                        .as("timeMatrix[%d][%d] = points[%d] -> points[%d]", i, j, i, j)
                        .isEqualByComparingTo(BigDecimal.valueOf(i * 100 + j));
            }
        }
    }

    @Test
    void reutilizaResultadosDeMapsParaElMismoParDentroDeLaMismaSesion() {
        DeliveryPoint p0 = point(0);
        DeliveryPoint p1 = point(1);
        DeliveryPoint p2 = point(2);
        List<DeliveryPoint> points = List.of(p0, p1, p2);

        when(pointDistanceService.getDistanceAndTime(any(), any()))
                .thenAnswer(invocation -> {
                    DeliveryPoint origin = invocation.getArgument(0);
                    DeliveryPoint destination = invocation.getArgument(1);
                    int i = index(origin);
                    int j = index(destination);
                    return new DistanceTimeResult(
                            BigDecimal.valueOf(i * 10 + j),
                            BigDecimal.valueOf(i * 100 + j));
                });

        DistanceMatrixService service = new DistanceMatrixService(pointDistanceService);
        MapsQueryCache cache = new MapsQueryCache();

        DistanceMatrixResult first = service.buildMatrix(points, cache, "sin-trafico");
        // Misma caché y mismo contexto de tráfico -> no debe consultar de nuevo al proveedor.
        DistanceMatrixResult second = service.buildMatrix(points, cache, "sin-trafico");

        assertThat(second.distanceMatrix()).isEqualTo(first.distanceMatrix());
        assertThat(second.timeMatrix()).isEqualTo(first.timeMatrix());
        verify(pointDistanceService, times(9)).getDistanceAndTime(any(), any());
    }

    @Test
    void consultaDeNuevoAlProveedorSiElContextoDeTraficoCambia() {
        DeliveryPoint p0 = point(0);
        DeliveryPoint p1 = point(1);
        List<DeliveryPoint> points = List.of(p0, p1);

        when(pointDistanceService.getDistanceAndTime(any(), any()))
                .thenReturn(new DistanceTimeResult(BigDecimal.ONE, BigDecimal.ONE));

        DistanceMatrixService service = new DistanceMatrixService(pointDistanceService);
        MapsQueryCache cache = new MapsQueryCache();

        service.buildMatrix(points, cache, "trafico-normal");
        service.buildMatrix(points, cache, "trafico-pesado");

        // 2x2 direcciones por cada contexto de tráfico distinto = 8 llamadas.
        verify(pointDistanceService, times(8)).getDistanceAndTime(any(), any());
    }

    private DeliveryPoint point(int index) {
        return DeliveryPoint.builder()
                .id(UUID.randomUUID())
                .reference("P" + index)
                .latitude(BigDecimal.valueOf(20 + index))
                .longitude(BigDecimal.valueOf(-103 - index))
                .build();
    }

    private int index(DeliveryPoint point) {
        return Integer.parseInt(point.getReference().substring(1));
    }
}
