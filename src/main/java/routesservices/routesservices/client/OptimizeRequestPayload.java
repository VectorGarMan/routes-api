package routesservices.routesservices.client;

import routesservices.routesservices.dto.DeliveryPointResponse;

import java.math.BigDecimal;
import java.util.List;

/** Cuerpo exacto de OptimizeRequest (CTR-001) enviado a POST /optimize (Python). */
public record OptimizeRequestPayload(
        String requestId,
        List<DeliveryPointResponse> points,
        BigDecimal[][] distanceMatrix,
        BigDecimal[][] timeMatrix,
        String objective,
        int depotIndex,
        List<Object> timeWindows
) {
}
