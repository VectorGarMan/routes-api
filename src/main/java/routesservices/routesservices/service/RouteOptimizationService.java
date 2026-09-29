package routesservices.routesservices.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import routesservices.routesservices.client.MapboxDirectionsClient;
import routesservices.routesservices.client.OptimizeRequestPayload;
import routesservices.routesservices.client.OptimizeResponsePayload;
import routesservices.routesservices.client.PythonOptimizerClient;
import routesservices.routesservices.dto.DeliveryPointResponse;
import routesservices.routesservices.dto.DistanceMatrixResult;
import routesservices.routesservices.dto.OptimizeRouteRequest;
import routesservices.routesservices.dto.RouteResponseDto;
import routesservices.routesservices.entity.DeliveryPoint;
import routesservices.routesservices.entity.Route;
import routesservices.routesservices.entity.RouteStop;
import routesservices.routesservices.exception.OptimizerUnavailableException;
import routesservices.routesservices.exception.RouteInfeasibleException;
import routesservices.routesservices.repository.DeliveryPointRepository;
import routesservices.routesservices.repository.RouteRepository;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Endpoint de negocio de BE-008/BE-009: valida los puntos, construye las
 * matrices, delega el cálculo al servicio Python, y persiste la ruta
 * resultante (routes + route_stops) exactamente como la devolvió Python.
 */
@Service
public class RouteOptimizationService {

    private static final Logger log = LoggerFactory.getLogger(RouteOptimizationService.class);
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final DeliveryPointRepository deliveryPointRepository;
    private final DistanceMatrixService distanceMatrixService;
    private final PythonOptimizerClient pythonOptimizerClient;
    private final RouteRepository routeRepository;
    private final MapboxDirectionsClient directionsClient;

    public RouteOptimizationService(DeliveryPointRepository deliveryPointRepository,
                                     DistanceMatrixService distanceMatrixService,
                                     PythonOptimizerClient pythonOptimizerClient,
                                     RouteRepository routeRepository,
                                     MapboxDirectionsClient directionsClient) {
        this.deliveryPointRepository = deliveryPointRepository;
        this.distanceMatrixService = distanceMatrixService;
        this.pythonOptimizerClient = pythonOptimizerClient;
        this.routeRepository = routeRepository;
        this.directionsClient = directionsClient;
    }

    @Transactional
    public RouteResponseDto optimize(OptimizeRouteRequest request) {
        List<UUID> pointIds = request.pointIds().stream().map(UUID::fromString).toList();
        List<DeliveryPoint> orderedPoints = loadPointsInRequestedOrder(pointIds);
        Map<UUID, DeliveryPoint> pointsById = orderedPoints.stream()
                .collect(Collectors.toMap(DeliveryPoint::getId, point -> point));

        int depotIndex = pointIds.indexOf(UUID.fromString(request.depotPointId()));
        if (depotIndex < 0) {
            throw new IllegalArgumentException("depotPointId debe estar incluido en pointIds");
        }

        DistanceMatrixResult matrices = distanceMatrixService.buildMatrix(
                orderedPoints, new MapsQueryCache(), TrafficContext.REAL_TIME);

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

        Route route = buildRoute(request, response, pointsById, matrices);
        Route saved = routeRepository.save(route);

        return RouteResponseMapper.toRouteResponse(saved);
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

    private Route buildRoute(OptimizeRouteRequest request, OptimizeResponsePayload response,
                              Map<UUID, DeliveryPoint> pointsById, DistanceMatrixResult matrices) {
        if ("INFEASIBLE".equals(response.status())) {
            throw new RouteInfeasibleException("No fue posible calcular una ruta viable con los puntos y restricciones dados");
        }
        if (!"SUCCESS".equals(response.status())) {
            throw new OptimizerUnavailableException("El optimizador devolvió un estado inesperado");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Route route = Route.builder()
                .requestId(UUID.fromString(response.requestId()))
                .status("ACTIVE")
                .objective(request.objective())
                .depotPointId(UUID.fromString(request.depotPointId()))
                .totalDistanceMeters(BigDecimal.valueOf(response.totalDistanceMeters()))
                .totalTimeSeconds(BigDecimal.valueOf(response.totalTimeSeconds()))
                .createdAt(now)
                .updatedAt(now)
                .build();

        List<RouteStop> stops = new ArrayList<>();
        List<String> pointOrder = response.route();
        List<Integer> matrixIndexes = response.routeIndexes();
        for (int order = 0; order < pointOrder.size(); order++) {
            UUID pointId = UUID.fromString(pointOrder.get(order));
            DeliveryPoint point = pointsById.get(pointId);
            if (point == null) {
                throw new OptimizerUnavailableException("El optimizador devolvió un punto que no forma parte de la solicitud");
            }

            int currentMatrixIndex = matrixIndexes.get(order);
            int previousMatrixIndex = order == 0 ? currentMatrixIndex : matrixIndexes.get(order - 1);
            BigDecimal legSeconds = matrices.timeMatrix()[previousMatrixIndex][currentMatrixIndex];

            stops.add(RouteStop.builder()
                    .route(route)
                    .point(point)
                    .stopOrder(order)
                    .status("PENDING")
                    .estimatedLegSeconds(legSeconds)
                    .build());
        }
        route.setStops(stops);
        route.setRouteGeometry(fetchGeometryJson(stops));
        return route;
    }

    /** Geometría real (calles) de la ruta completa, en el orden final de paradas. */
    private String fetchGeometryJson(List<RouteStop> orderedStops) {
        List<BigDecimal[]> latLngPoints = orderedStops.stream()
                .map(stop -> new BigDecimal[]{stop.getPoint().getLatitude(), stop.getPoint().getLongitude()})
                .toList();
        try {
            List<List<Double>> geometry = directionsClient.getRouteGeometry(latLngPoints, true);
            return OBJECT_MAPPER.writeValueAsString(geometry);
        } catch (Exception ex) {
            log.warn("No se pudo obtener la geometría de la ruta desde Mapbox; se guarda la ruta sin geometría", ex);
            return null;
        }
    }
}
