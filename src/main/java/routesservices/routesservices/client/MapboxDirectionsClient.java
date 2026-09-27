package routesservices.routesservices.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import routesservices.routesservices.exception.MapsRateLimitException;
import routesservices.routesservices.exception.MapsUnavailableException;

import java.math.BigDecimal;

/**
 * Distancia y tiempo de viaje entre un par de puntos (dirección origen -> destino),
 * usando la Directions API de Mapbox.
 */
@Component
public class MapboxDirectionsClient {

    private final RestClient restClient;
    private final String accessToken;

    public MapboxDirectionsClient(RestClient.Builder builder,
                                   @Value("${maps.mapbox.base-url}") String baseUrl,
                                   @Value("${maps.mapbox.access-token}") String accessToken) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.accessToken = accessToken;
    }

    public DistanceTimeResult getDistanceAndDuration(BigDecimal originLatitude, BigDecimal originLongitude,
                                                      BigDecimal destinationLatitude, BigDecimal destinationLongitude) {
        String coordinates = originLongitude.toPlainString() + "," + originLatitude.toPlainString()
                + ";" + destinationLongitude.toPlainString() + "," + destinationLatitude.toPlainString();

        try {
            MapboxDirectionsResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/directions/v5/mapbox/driving/{coordinates}")
                            .queryParam("access_token", accessToken)
                            .queryParam("overview", "false")
                            .build(coordinates))
                    .retrieve()
                    .body(MapboxDirectionsResponse.class);
            return toResult(response);
        } catch (HttpClientErrorException.TooManyRequests ex) {
            throw new MapsRateLimitException("Límite de solicitudes a Mapbox excedido");
        } catch (RestClientException ex) {
            throw new MapsUnavailableException("El servicio de mapas (Mapbox) no está disponible", ex);
        }
    }

    private DistanceTimeResult toResult(MapboxDirectionsResponse response) {
        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            throw new MapsUnavailableException("Mapbox no devolvió una ruta entre los puntos solicitados");
        }

        MapboxDirectionsResponse.MapboxRoute route = response.routes().get(0);
        return new DistanceTimeResult(
                BigDecimal.valueOf(route.distance()),
                BigDecimal.valueOf(route.duration())
        );
    }
}
