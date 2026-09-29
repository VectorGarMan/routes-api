package routesservices.routesservices.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routesservices.routesservices.dto.RouteResponseDto;
import routesservices.routesservices.dto.RouteStopDto;
import routesservices.routesservices.entity.Route;
import routesservices.routesservices.entity.RouteStop;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Comparator;
import java.util.List;

/** Mapeo compartido Route -> RouteResponseDto (usado por cálculo, recálculo e historial). */
final class RouteResponseMapper {

    private static final Logger log = LoggerFactory.getLogger(RouteResponseMapper.class);
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

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
                parseGeometry(route.getRouteGeometry()),
                route.getUpdatedAt()
        );
    }

    private static List<List<Double>> parseGeometry(String routeGeometryJson) {
        if (routeGeometryJson == null || routeGeometryJson.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(routeGeometryJson, new TypeReference<List<List<Double>>>() {
            });
        } catch (Exception ex) {
            log.warn("No se pudo parsear route_geometry almacenada, se omite del response", ex);
            return null;
        }
    }
}
