package routesservices.routesservices.service;

import org.springframework.stereotype.Component;
import routesservices.routesservices.client.DistanceTimeResult;
import routesservices.routesservices.entity.DeliveryPoint;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Datos históricos/simulados de tráfico (BE-010), sin llamar a Mapbox: evita
 * cargos duplicados durante pruebas, según la regla del backlog. Estima la
 * distancia en línea recta (haversine) y aplica un factor de tráfico fijo
 * sobre una velocidad promedio de flujo libre, para representar de forma
 * reproducible un tiempo "afectado por tráfico".
 */
@Component
public class SimulatedTrafficProvider {

    private static final double EARTH_RADIUS_METERS = 6_371_000;
    static final BigDecimal FREE_FLOW_SPEED_METERS_PER_SECOND = BigDecimal.valueOf(11.11); // ~40 km/h
    static final BigDecimal SIMULATED_TRAFFIC_FACTOR = BigDecimal.valueOf(1.6); // +60% por congestión simulada

    public DistanceTimeResult getDistanceAndTime(DeliveryPoint origin, DeliveryPoint destination) {
        BigDecimal distanceMeters = haversineMeters(origin, destination);
        BigDecimal freeFlowSeconds = distanceMeters.divide(FREE_FLOW_SPEED_METERS_PER_SECOND, 0, RoundingMode.HALF_UP);
        BigDecimal durationWithTraffic = freeFlowSeconds.multiply(SIMULATED_TRAFFIC_FACTOR)
                .setScale(0, RoundingMode.HALF_UP);

        return new DistanceTimeResult(distanceMeters, durationWithTraffic);
    }

    private BigDecimal haversineMeters(DeliveryPoint origin, DeliveryPoint destination) {
        double lat1 = Math.toRadians(origin.getLatitude().doubleValue());
        double lat2 = Math.toRadians(destination.getLatitude().doubleValue());
        double deltaLat = Math.toRadians(destination.getLatitude().subtract(origin.getLatitude()).doubleValue());
        double deltaLng = Math.toRadians(destination.getLongitude().subtract(origin.getLongitude()).doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return BigDecimal.valueOf(EARTH_RADIUS_METERS * c);
    }
}
