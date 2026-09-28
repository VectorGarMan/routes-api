package routesservices.routesservices.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Ventana de tiempo en la que se puede realizar la entrega")
public record TimeWindowDto(
        @Schema(description = "Inicio de la ventana (ISO-8601 con offset)", example = "2025-06-10T08:00:00-06:00")
        OffsetDateTime start,

        @Schema(description = "Fin de la ventana (ISO-8601 con offset)", example = "2025-06-10T12:00:00-06:00")
        OffsetDateTime end
) {
}
