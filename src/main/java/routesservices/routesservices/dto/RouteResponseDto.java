package routesservices.routesservices.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record RouteResponseDto(
        String routeId,
        String status,
        List<RouteStopDto> stops,
        BigDecimal totalDistanceMeters,
        BigDecimal totalTimeSeconds,
        OffsetDateTime updatedAt
) {
}
