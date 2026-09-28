package routesservices.routesservices.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import routesservices.routesservices.client.DistanceTimeResult;
import routesservices.routesservices.client.OptimizeRequestPayload;
import routesservices.routesservices.client.OptimizeResponsePayload;
import routesservices.routesservices.client.PythonOptimizerClient;
import routesservices.routesservices.dto.DeliveryPointResponse;
import routesservices.routesservices.dto.DistanceMatrixResult;
import routesservices.routesservices.dto.RouteResponseDto;
import routesservices.routesservices.entity.DeliveryPoint;
import routesservices.routesservices.entity.Route;
import routesservices.routesservices.entity.RouteStop;
import routesservices.routesservices.exception.OptimizerUnavailableException;
import routesservices.routesservices.exception.RouteInfeasibleException;
import routesservices.routesservices.repository.RouteRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * BE-011: detecta si el tiempo estimado de algún tramo pendiente cambió de
 * forma significativa (tráfico) respecto al valor guardado en el último
 * cálculo/recálculo, y si es así, recalcula la ruta excluyendo las paradas
 * ya visitadas. Sin cambio o cambio bajo (por debajo del umbral) -> se
 * devuelve la ruta sin modificar.
 */
@Service
public class RouteRecalculationService {

    /** Umbral de cambio significativo: diferencia relativa > 25% en el tiempo de un tramo. */
    static final BigDecimal SIGNIFICANT_CHANGE_THRESHOLD = BigDecimal.valueOf(0.25);

    private final RouteRepository routeRepository;
    private final PointDistanceService pointDistanceService;
    private final DistanceMatrixService distanceMatrixService;
    private final PythonOptimizerClient pythonOptimizerClient;

    public RouteRecalculationService(RouteRepository routeRepository,
                                      PointDistanceService pointDistanceService,
                                      DistanceMatrixService distanceMatrixService,
                                      PythonOptimizerClient pythonOptimizerClient) {
        this.routeRepository = routeRepository;
        this.pointDistanceService = pointDistanceService;
        this.distanceMatrixService = distanceMatrixService;
        this.pythonOptimizerClient = pythonOptimizerClient;
    }

    @Transactional
    public RouteResponseDto recalculateIfNeeded(UUID routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("routeId no existe"));

        List<RouteStop> orderedStops = route.getStops().stream()
                .sorted(Comparator.comparingInt(RouteStop::getStopOrder))
                .toList();
        List<RouteStop> visited = orderedStops.stream().filter(this::isVisited).toList();
        List<RouteStop> remaining = orderedStops.stream().filter(stop -> !isVisited(stop)).toList();

        if (remaining.isEmpty()) {
            return RouteResponseMapper.toRouteResponse(route);
        }

        DeliveryPoint currentPosition = visited.isEmpty()
                ? remaining.get(0).getPoint()
                : visited.get(visited.size() - 1).getPoint();

        if (!hasSignificantChange(currentPosition, remaining)) {
            return RouteResponseMapper.toRouteResponse(route);
        }

        recalculateRemaining(route, visited, remaining, currentPosition);
        Route saved = routeRepository.save(route);
        return RouteResponseMapper.toRouteResponse(saved);
    }

    private boolean isVisited(RouteStop stop) {
        return "VISITED".equals(stop.getStatus());
    }

    private boolean hasSignificantChange(DeliveryPoint currentPosition, List<RouteStop> remaining) {
        DeliveryPoint previousPoint = currentPosition;
        for (RouteStop stop : remaining) {
            DistanceTimeResult current = pointDistanceService.getDistanceAndTime(
                    previousPoint, stop.getPoint(), TrafficContext.REAL_TIME);
            if (isSignificantChange(stop.getEstimatedLegSeconds(), current.durationSeconds())) {
                return true;
            }
            previousPoint = stop.getPoint();
        }
        return false;
    }

    private boolean isSignificantChange(BigDecimal storedSeconds, BigDecimal currentSeconds) {
        if (storedSeconds == null || storedSeconds.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        BigDecimal relativeDifference = currentSeconds.subtract(storedSeconds).abs()
                .divide(storedSeconds, 4, RoundingMode.HALF_UP);
        return relativeDifference.compareTo(SIGNIFICANT_CHANGE_THRESHOLD) > 0;
    }

    private void recalculateRemaining(Route route, List<RouteStop> visited, List<RouteStop> remaining,
                                       DeliveryPoint currentPosition) {
        boolean includeCurrentPositionAsPseudoDepot = !visited.isEmpty();

        List<DeliveryPoint> pointsForRecalc = new ArrayList<>();
        if (includeCurrentPositionAsPseudoDepot) {
            pointsForRecalc.add(currentPosition);
        }
        remaining.forEach(stop -> pointsForRecalc.add(stop.getPoint()));

        DistanceMatrixResult matrices = distanceMatrixService.buildMatrix(
                pointsForRecalc, new MapsQueryCache(), TrafficContext.REAL_TIME);

        String requestId = UUID.randomUUID().toString();
        List<DeliveryPointResponse> pythonPoints = pointsForRecalc.stream()
                .map(DeliveryPointResponse::from)
                .toList();

        OptimizeRequestPayload payload = new OptimizeRequestPayload(
                requestId, pythonPoints, matrices.distanceMatrix(), matrices.timeMatrix(),
                route.getObjective(), 0, null);

        OptimizeResponsePayload response = pythonOptimizerClient.optimize(payload);

        if ("INFEASIBLE".equals(response.status())) {
            throw new RouteInfeasibleException("No fue posible recalcular una ruta viable con los puntos restantes");
        }
        if (!"SUCCESS".equals(response.status())) {
            throw new OptimizerUnavailableException("El optimizador devolvió un estado inesperado durante el recálculo");
        }

        // Nunca se elimina lo ya visitado: solo se reemplazan las paradas pendientes/actuales.
        route.getStops().removeIf(stop -> !isVisited(stop));

        List<String> newOrder = response.route();
        List<Integer> newIndexes = response.routeIndexes();
        int startOffset = includeCurrentPositionAsPseudoDepot ? 1 : 0;
        int nextStopOrder = visited.size();

        for (int i = startOffset; i < newOrder.size(); i++) {
            UUID pointId = UUID.fromString(newOrder.get(i));
            DeliveryPoint point = findPointById(pointsForRecalc, pointId);

            int currentMatrixIndex = newIndexes.get(i);
            int previousMatrixIndex = newIndexes.get(i - 1);
            BigDecimal legSeconds = matrices.timeMatrix()[previousMatrixIndex][currentMatrixIndex];

            route.getStops().add(RouteStop.builder()
                    .route(route)
                    .point(point)
                    .stopOrder(nextStopOrder++)
                    .status("PENDING")
                    .estimatedLegSeconds(legSeconds)
                    .build());
        }

        route.setTotalDistanceMeters(BigDecimal.valueOf(response.totalDistanceMeters()));
        route.setTotalTimeSeconds(BigDecimal.valueOf(response.totalTimeSeconds()));
        route.setUpdatedAt(OffsetDateTime.now());
    }

    private DeliveryPoint findPointById(List<DeliveryPoint> points, UUID pointId) {
        return points.stream()
                .filter(point -> point.getId().equals(pointId))
                .findFirst()
                .orElseThrow(() -> new OptimizerUnavailableException(
                        "El optimizador devolvió un punto que no forma parte del recálculo"));
    }

}
