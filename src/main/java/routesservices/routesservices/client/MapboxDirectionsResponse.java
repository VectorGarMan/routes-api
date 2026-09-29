package routesservices.routesservices.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record MapboxDirectionsResponse(List<MapboxRoute> routes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MapboxRoute(double distance, double duration, MapboxGeometry geometry) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MapboxGeometry(List<List<Double>> coordinates) {
    }
}
