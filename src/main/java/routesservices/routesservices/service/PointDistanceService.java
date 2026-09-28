package routesservices.routesservices.service;

import org.springframework.stereotype.Service;
import routesservices.routesservices.client.DistanceTimeResult;
import routesservices.routesservices.client.MapboxDirectionsClient;
import routesservices.routesservices.entity.DeliveryPoint;

import java.math.BigDecimal;

/**
 * Distancia y tiempo entre dos puntos de entrega (dirección i -> j). BE-006 la usa
 * para construir la matriz NxN completa; BE-007 le agrega caché; BE-010 agrega el
 * contexto de tráfico (real, sin tráfico, o simulado/histórico sin llamar a Mapbox).
 */
@Service
public class PointDistanceService {

    private final MapboxDirectionsClient directionsClient;
    private final SimulatedTrafficProvider simulatedTrafficProvider;

    public PointDistanceService(MapboxDirectionsClient directionsClient,
                                 SimulatedTrafficProvider simulatedTrafficProvider) {
        this.directionsClient = directionsClient;
        this.simulatedTrafficProvider = simulatedTrafficProvider;
    }

    public DistanceTimeResult getDistanceAndTime(DeliveryPoint origin, DeliveryPoint destination,
                                                  TrafficContext trafficContext) {
        if (origin.getId().equals(destination.getId())) {
            return new DistanceTimeResult(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        if (trafficContext == TrafficContext.SIMULATED) {
            return simulatedTrafficProvider.getDistanceAndTime(origin, destination);
        }

        boolean withTraffic = trafficContext == TrafficContext.REAL_TIME;
        return directionsClient.getDistanceAndDuration(
                origin.getLatitude(), origin.getLongitude(),
                destination.getLatitude(), destination.getLongitude(),
                withTraffic
        );
    }
}
