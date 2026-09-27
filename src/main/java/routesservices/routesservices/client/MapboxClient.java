package routesservices.routesservices.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import routesservices.routesservices.exception.MapsRateLimitException;
import routesservices.routesservices.exception.MapsUnavailableException;

import java.math.BigDecimal;
import java.util.List;

@Component
public class MapboxClient {

    private final RestClient restClient;
    private final String accessToken;

    public MapboxClient(RestClient.Builder builder,
                         @Value("${maps.mapbox.base-url}") String baseUrl,
                         @Value("${maps.mapbox.access-token}") String accessToken) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.accessToken = accessToken;
    }

    public GeocodeResult geocode(String address) {
        return toResult(fetch(address));
    }

    public GeocodeResult reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        String query = longitude.toPlainString() + "," + latitude.toPlainString();
        return toResult(fetch(query));
    }

    private MapboxGeocodeResponse fetch(String query) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/geocoding/v5/mapbox.places/{query}.json")
                            .queryParam("access_token", accessToken)
                            .queryParam("limit", 1)
                            .build(query))
                    .retrieve()
                    .body(MapboxGeocodeResponse.class);
        } catch (HttpClientErrorException.TooManyRequests ex) {
            throw new MapsRateLimitException("Límite de solicitudes a Mapbox excedido");
        } catch (RestClientException ex) {
            throw new MapsUnavailableException("El servicio de mapas (Mapbox) no está disponible", ex);
        }
    }

    private GeocodeResult toResult(MapboxGeocodeResponse response) {
        if (response == null || response.features() == null || response.features().isEmpty()) {
            return GeocodeResult.invalid();
        }

        MapboxGeocodeResponse.MapboxFeature first = response.features().get(0);
        List<Double> coordinates = first.geometry().coordinates();
        BigDecimal longitude = BigDecimal.valueOf(coordinates.get(0));
        BigDecimal latitude = BigDecimal.valueOf(coordinates.get(1));

        return GeocodeResult.valid(latitude, longitude, first.placeName());
    }
}
