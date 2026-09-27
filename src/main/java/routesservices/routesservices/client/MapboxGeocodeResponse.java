package routesservices.routesservices.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record MapboxGeocodeResponse(List<MapboxFeature> features) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MapboxFeature(@JsonProperty("place_name") String placeName, MapboxGeometry geometry) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MapboxGeometry(List<Double> coordinates) {
    }
}
