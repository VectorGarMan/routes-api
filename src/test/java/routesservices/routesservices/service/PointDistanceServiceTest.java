package routesservices.routesservices.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import routesservices.routesservices.client.DistanceTimeResult;
import routesservices.routesservices.client.MapboxDirectionsClient;
import routesservices.routesservices.entity.DeliveryPoint;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * BE-010: verifica que el contexto de tráfico decide correctamente la fuente
 * de datos (perfil de Mapbox real, o simulado sin llamar a Mapbox).
 */
@ExtendWith(MockitoExtension.class)
class PointDistanceServiceTest {

    @Mock
    private MapboxDirectionsClient directionsClient;

    private final SimulatedTrafficProvider simulatedTrafficProvider = new SimulatedTrafficProvider();

    @Test
    void trafficoSimuladoNuncaLlamaAMapbox() {
        DeliveryPoint origin = point(20.5200, -103.3200);
        DeliveryPoint destination = point(20.6800, -103.3500);
        PointDistanceService service = new PointDistanceService(directionsClient, simulatedTrafficProvider);

        DistanceTimeResult result = service.getDistanceAndTime(origin, destination, TrafficContext.SIMULATED);

        assertThat(result.distanceMeters()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.durationSeconds()).isGreaterThan(BigDecimal.ZERO);
        verifyNoInteractions(directionsClient);
    }

    @Test
    void traficoEnTiempoRealUsaElPerfilConTrafico() {
        DeliveryPoint origin = point(20.5200, -103.3200);
        DeliveryPoint destination = point(20.6800, -103.3500);
        when(directionsClient.getDistanceAndDuration(any(), any(), any(), any(), eq(true)))
                .thenReturn(new DistanceTimeResult(BigDecimal.valueOf(1000), BigDecimal.valueOf(500)));

        PointDistanceService service = new PointDistanceService(directionsClient, simulatedTrafficProvider);
        DistanceTimeResult result = service.getDistanceAndTime(origin, destination, TrafficContext.REAL_TIME);

        assertThat(result.durationSeconds()).isEqualByComparingTo(BigDecimal.valueOf(500));
        verify(directionsClient).getDistanceAndDuration(any(), any(), any(), any(), eq(true));
    }

    @Test
    void sinTraficoUsaElPerfilSinTrafico() {
        DeliveryPoint origin = point(20.5200, -103.3200);
        DeliveryPoint destination = point(20.6800, -103.3500);
        when(directionsClient.getDistanceAndDuration(any(), any(), any(), any(), eq(false)))
                .thenReturn(new DistanceTimeResult(BigDecimal.valueOf(1000), BigDecimal.valueOf(300)));

        PointDistanceService service = new PointDistanceService(directionsClient, simulatedTrafficProvider);
        DistanceTimeResult result = service.getDistanceAndTime(origin, destination, TrafficContext.NONE);

        assertThat(result.durationSeconds()).isEqualByComparingTo(BigDecimal.valueOf(300));
        verify(directionsClient).getDistanceAndDuration(any(), any(), any(), any(), eq(false));
    }

    private DeliveryPoint point(double lat, double lng) {
        return DeliveryPoint.builder()
                .id(UUID.randomUUID())
                .reference("Punto")
                .latitude(BigDecimal.valueOf(lat))
                .longitude(BigDecimal.valueOf(lng))
                .build();
    }
}
