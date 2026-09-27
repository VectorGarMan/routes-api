package routesservices.routesservices.service;

import org.springframework.stereotype.Service;
import routesservices.routesservices.client.DistanceTimeResult;
import routesservices.routesservices.client.MapboxDirectionsClient;
import routesservices.routesservices.entity.DeliveryPoint;

import java.math.BigDecimal;

/**
 * Distancia y tiempo entre dos puntos de entrega (dirección i -> j). BE-006 la usa
 * para construir la matriz NxN completa; BE-007 le agrega caché.
 */
@Service
public class PointDistanceService {

    private final MapboxDirectionsClient directionsClient;

    public PointDistanceService(MapboxDirectionsClient directionsClient) {
        this.directionsClient = directionsClient;
    }

    public DistanceTimeResult getDistanceAndTime(DeliveryPoint origin, DeliveryPoint destination) {
        if (origin.getId().equals(destination.getId())) {
            return new DistanceTimeResult(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        return directionsClient.getDistanceAndDuration(
                origin.getLatitude(), origin.getLongitude(),
                destination.getLatitude(), destination.getLongitude()
        );
    }
}
