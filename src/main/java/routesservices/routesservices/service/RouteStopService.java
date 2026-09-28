package routesservices.routesservices.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import routesservices.routesservices.dto.RouteResponseDto;
import routesservices.routesservices.entity.Route;
import routesservices.routesservices.entity.RouteStop;
import routesservices.routesservices.repository.RouteRepository;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * BE-013: marca una parada como visitada (paso previo indispensable para que
 * BE-011 tenga paradas VISITED reales que excluir). Las paradas deben
 * marcarse en orden: no se puede saltar una pendiente anterior.
 */
@Service
public class RouteStopService {

    private final RouteRepository routeRepository;

    public RouteStopService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Transactional
    public RouteResponseDto markVisited(UUID routeId, UUID pointId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("routeId no existe"));

        List<RouteStop> orderedStops = route.getStops().stream()
                .sorted(Comparator.comparingInt(RouteStop::getStopOrder))
                .toList();

        RouteStop target = orderedStops.stream()
                .filter(stop -> stop.getPoint().getId().equals(pointId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("pointId no forma parte de esta ruta"));

        if ("VISITED".equals(target.getStatus())) {
            // Idempotente: reintentar marcar una parada ya visitada no es un error.
            return RouteResponseMapper.toRouteResponse(route);
        }

        RouteStop nextPending = orderedStops.stream()
                .filter(stop -> !"VISITED".equals(stop.getStatus()))
                .min(Comparator.comparingInt(RouteStop::getStopOrder))
                .orElseThrow(() -> new IllegalStateException("La ruta no tiene paradas pendientes"));

        if (!nextPending.getPoint().getId().equals(pointId)) {
            throw new IllegalArgumentException(
                    "Debe marcarse primero la parada pendiente con order=" + nextPending.getStopOrder());
        }

        target.setStatus("VISITED");
        target.setVisitedAt(OffsetDateTime.now());

        boolean allVisited = orderedStops.stream().allMatch(stop -> "VISITED".equals(stop.getStatus()));
        if (allVisited) {
            route.setStatus("COMPLETED");
        }
        route.setUpdatedAt(OffsetDateTime.now());

        Route saved = routeRepository.save(route);
        return RouteResponseMapper.toRouteResponse(saved);
    }
}
