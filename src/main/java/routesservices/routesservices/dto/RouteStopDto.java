package routesservices.routesservices.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Parada individual dentro de una ruta")
public record RouteStopDto(
        @Schema(description = "ID del punto de entrega", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
        String pointId,

        @Schema(description = "Posición en la secuencia de visita (0 = depósito)", example = "1")
        int order,

        @Schema(description = "Estado de la parada: PENDING, VISITED", example = "PENDING")
        String status
) {
}
