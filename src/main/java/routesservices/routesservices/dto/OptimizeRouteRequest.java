package routesservices.routesservices.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

@Schema(description = "Solicitud para calcular la ruta óptima entre un conjunto de puntos de entrega")
public record OptimizeRouteRequest(
        @NotEmpty
        @Schema(
                description = "Lista de IDs de puntos de entrega a incluir en la ruta (mínimo 2, incluyendo el depósito)",
                example = "[\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\", \"b2c3d4e5-f6a7-8901-bcde-f12345678901\", \"c3d4e5f6-a7b8-9012-cdef-123456789012\"]"
        )
        List<@NotBlank String> pointIds,

        @NotBlank @Pattern(regexp = "DISTANCE|TIME")
        @Schema(
                description = "Criterio de optimización: DISTANCE minimiza kilómetros recorridos, TIME minimiza tiempo total",
                example = "DISTANCE",
                allowableValues = {"DISTANCE", "TIME"}
        )
        String objective,

        @NotBlank
        @Schema(
                description = "ID del punto que actúa como depósito (punto de partida y llegada de la ruta). Debe estar incluido en pointIds",
                example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        )
        String depotPointId
) {
}
