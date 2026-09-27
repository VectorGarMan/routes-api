package routesservices.routesservices.client;

import java.math.BigDecimal;

/** distanceMeters y durationSeconds, nunca kilómetros/minutos (conversión solo en presentación). */
public record DistanceTimeResult(BigDecimal distanceMeters, BigDecimal durationSeconds) {
}
