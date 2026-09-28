package routesservices.routesservices.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Ruta calculada u obtenida del historial")
public record RouteResponseDto(
        @Schema(description = "Identificador único de la ruta", example = "d4e5f6a7-b8c9-0123-defa-456789012345")
        String routeId,

        @Schema(description = "Estado de la ruta: PENDING, IN_PROGRESS, COMPLETED", example = "PENDING")
        String status,

        @Schema(description = "Paradas en el orden optimizado")
        List<RouteStopDto> stops,

        @Schema(description = "Distancia total de la ruta en metros", example = "18450.50")
        BigDecimal totalDistanceMeters,

        @Schema(description = "Tiempo total estimado de la ruta en segundos", example = "3240.00")
        BigDecimal totalTimeSeconds,

        @Schema(description = "Fecha y hora de la última actualización (ISO-8601)", example = "2025-06-10T09:15:00-06:00")
        OffsetDateTime updatedAt
) {
}
