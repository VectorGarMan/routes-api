package routesservices.routesservices.service;

import org.junit.jupiter.api.Test;
import routesservices.routesservices.client.DistanceTimeResult;
import routesservices.routesservices.entity.DeliveryPoint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BE-010: demuestra que timeMatrix (vía este proveedor) refleja el tiempo
 * afectado por tráfico: la duración simulada es siempre mayor que la de
 * flujo libre, por el factor de congestión aplicado.
 */
class SimulatedTrafficProviderTest {

    private final SimulatedTrafficProvider provider = new SimulatedTrafficProvider();

    @Test
    void laDuracionSimuladaEsMayorQueLaDeFlujoLibrePorElFactorDeTrafico() {
        DeliveryPoint origin = point(20.5200, -103.3200);
        DeliveryPoint destination = point(20.6800, -103.3500);

        DistanceTimeResult result = provider.getDistanceAndTime(origin, destination);

        BigDecimal duracionFlujoLibre = result.distanceMeters()
                .divide(SimulatedTrafficProvider.FREE_FLOW_SPEED_METERS_PER_SECOND, 0, RoundingMode.HALF_UP);
        BigDecimal duracionEsperadaConTrafico = duracionFlujoLibre
                .multiply(SimulatedTrafficProvider.SIMULATED_TRAFFIC_FACTOR)
                .setScale(0, RoundingMode.HALF_UP);

        assertThat(result.distanceMeters()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.durationSeconds()).isGreaterThan(duracionFlujoLibre);
        assertThat(result.durationSeconds()).isEqualByComparingTo(duracionEsperadaConTrafico);
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
