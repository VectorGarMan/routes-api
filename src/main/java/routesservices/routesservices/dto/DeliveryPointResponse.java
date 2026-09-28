package routesservices.routesservices.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import routesservices.routesservices.entity.DeliveryPoint;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Punto de entrega registrado en el sistema")
public record DeliveryPointResponse(
        @Schema(description = "Identificador único del punto", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "Nombre o referencia del punto", example = "Tienda Norte #42")
        String reference,

        @Schema(description = "Dirección legible", example = "Av. Vallarta 1234, Guadalajara, Jalisco")
        String address,

        @Schema(description = "Latitud WGS-84", example = "20.6597")
        BigDecimal latitude,

        @Schema(description = "Longitud WGS-84", example = "-103.3496")
        BigDecimal longitude,

        @Schema(description = "Ventana de tiempo configurada (null si no se definió)")
        TimeWindowDto timeWindow
) {

    public static DeliveryPointResponse from(DeliveryPoint entity) {
        TimeWindowDto timeWindow = entity.getTimeWindow() == null
                ? null
                : new TimeWindowDto(entity.getTimeWindow().getStart(), entity.getTimeWindow().getEnd());

        return new DeliveryPointResponse(
                entity.getId(),
                entity.getReference(),
                entity.getAddress(),
                entity.getLatitude(),
                entity.getLongitude(),
                timeWindow
        );
    }
}
