package routesservices.routesservices.service;

import routesservices.routesservices.dto.RouteResponseDto;
import routesservices.routesservices.dto.RouteStopDto;
import routesservices.routesservices.entity.Route;
import routesservices.routesservices.entity.RouteStop;

import java.util.Comparator;
import java.util.List;

/** Mapeo compartido Route -> RouteResponseDto (usado por cálculo, recálculo e historial). */
final class RouteResponseMapper {

    private RouteResponseMapper() {
    }

    static RouteResponseDto toRouteResponse(Route route) {
        List<RouteStopDto> stops = route.getStops().stream()
                .sorted(Comparator.comparingInt(RouteStop::getStopOrder))
                .map(stop -> new RouteStopDto(stop.getPoint().getId().toString(), stop.getStopOrder(), stop.getStatus()))
                .toList();

        return new RouteResponseDto(
                route.getId().toString(),
                route.getStatus(),
                stops,
                route.getTotalDistanceMeters(),
                route.getTotalTimeSeconds(),
                route.getUpdatedAt()
        );
    }
}
