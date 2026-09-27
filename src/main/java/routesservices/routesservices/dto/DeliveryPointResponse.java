package routesservices.routesservices.dto;

import routesservices.routesservices.entity.DeliveryPoint;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryPointResponse(
        UUID id,
        String reference,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
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
