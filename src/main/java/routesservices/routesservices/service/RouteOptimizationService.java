package routesservices.routesservices.service;

import org.springframework.stereotype.Service;
import routesservices.routesservices.client.OptimizeRequestPayload;
import routesservices.routesservices.client.OptimizeResponsePayload;
import routesservices.routesservices.client.PythonOptimizerClient;
import routesservices.routesservices.dto.DeliveryPointResponse;
import routesservices.routesservices.dto.DistanceMatrixResult;
import routesservices.routesservices.dto.OptimizeRouteRequest;
import routesservices.routesservices.dto.RouteResponseDto;
import routesservices.routesservices.dto.RouteStopDto;
import routesservices.routesservices.entity.DeliveryPoint;
import routesservices.routesservices.exception.OptimizerUnavailableException;
import routesservices.routesservices.exception.RouteInfeasibleException;
import routesservices.routesservices.repository.DeliveryPointRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Endpoint de negocio de BE-008: valida los puntos, construye las matrices,
 * genera un requestId y delega el cálculo al servicio Python. No persiste la
 * ruta todavía (eso es BE-009); "No exponer detalles internos de Python a
 * React" se traduce aquí a RouteInfeasibleException/OptimizerUnavailableException.
 */
@Service
public class RouteOptimizationService {

    private final DeliveryPointRepository deliveryPointRepository;
    private final DistanceMatrixService distanceMatrixService;
    private final PythonOptimizerClient pythonOptimizerClient;

    public RouteOptimizationService(DeliveryPointRepository deliveryPointRepository,
                                     DistanceMatrixService distanceMatrixService,
                                     PythonOptimizerClient pythonOptimizerClient) {
        this.deliveryPointRepository = deliveryPointRepository;
        this.distanceMatrixService = distanceMatrixService;
        this.pythonOptimizerClient = pythonOptimizerClient;
    }

    public RouteResponseDto optimize(OptimizeRouteRequest request) {
        List<UUID> pointIds = request.pointIds().stream().map(UUID::fromString).toList();
        List<DeliveryPoint> orderedPoints = loadPointsInRequestedOrder(pointIds);

        int depotIndex = pointIds.indexOf(UUID.fromString(request.depotPointId()));
        if (depotIndex < 0) {
            throw new IllegalArgumentException("depotPointId debe estar incluido en pointIds");
        }

        DistanceMatrixResult matrices = distanceMatrixService.buildMatrix(orderedPoints);

        String requestId = UUID.randomUUID().toString();
        List<DeliveryPointResponse> pythonPoints = orderedPoints.stream()
                .map(DeliveryPointResponse::from)
                .toList();

        OptimizeRequestPayload payload = new OptimizeRequestPayload(
                requestId,
                pythonPoints,
                matrices.distanceMatrix(),
                matrices.timeMatrix(),
                request.objective(),
                depotIndex,
                null
        );

        OptimizeResponsePayload response = pythonOptimizerClient.optimize(payload);
        return toRouteResponse(response);
    }

    private List<DeliveryPoint> loadPointsInRequestedOrder(List<UUID> pointIds) {
        List<DeliveryPoint> found = deliveryPointRepository.findAllById(pointIds);
        if (found.size() != pointIds.size()) {
            throw new IllegalArgumentException("Uno o más pointIds no existen");
        }

        Map<UUID, DeliveryPoint> byId = found.stream()
                .collect(Collectors.toMap(DeliveryPoint::getId, point -> point));
        return pointIds.stream().map(byId::get).toList();
    }

    private RouteResponseDto toRouteResponse(OptimizeResponsePayload response) {
        if ("INFEASIBLE".equals(response.status())) {
            throw new RouteInfeasibleException("No fue posible calcular una ruta viable con los puntos y restricciones dados");
        }
        if (!"SUCCESS".equals(response.status())) {
            throw new OptimizerUnavailableException("El optimizador devolvió un estado inesperado");
        }

        List<RouteStopDto> stops = new ArrayList<>();
        List<String> route = response.route();
        for (int order = 0; order < route.size(); order++) {
            stops.add(new RouteStopDto(route.get(order), order, "PENDING"));
        }

        return new RouteResponseDto(
                response.requestId(),
                "ACTIVE",
                stops,
                BigDecimal.valueOf(response.totalDistanceMeters()),
                BigDecimal.valueOf(response.totalTimeSeconds()),
                OffsetDateTime.now()
        );
    }
}
