package routesservices.routesservices.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

@Schema(description = "Datos para registrar un nuevo punto de entrega")
public record DeliveryPointRequest(
        @NotBlank
        @Schema(description = "Nombre o referencia del punto", example = "Tienda Norte #42")
        String reference,

        @Schema(description = "Dirección legible (opcional si se proporcionan coordenadas)", example = "Av. Vallarta 1234, Guadalajara, Jalisco")
        String address,

        @DecimalMin("-90") @DecimalMax("90")
        @Schema(description = "Latitud WGS-84", example = "20.6597")
        BigDecimal latitude,

        @DecimalMin("-180") @DecimalMax("180")
        @Schema(description = "Longitud WGS-84", example = "-103.3496")
        BigDecimal longitude,

        @Schema(description = "Ventana de tiempo para la entrega (opcional)")
        TimeWindowDto timeWindow
) {
}
