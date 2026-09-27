package routesservices.routesservices.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Cuerpo exacto de OptimizeResponse (CTR-001) devuelto por POST /optimize (Python). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OptimizeResponsePayload(
        String requestId,
        List<String> route,
        List<Integer> routeIndexes,
        double totalDistanceMeters,
        double totalTimeSeconds,
        String status,
        String errorCode,
        String message
) {
}
