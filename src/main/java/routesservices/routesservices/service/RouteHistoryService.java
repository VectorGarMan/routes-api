package routesservices.routesservices.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import routesservices.routesservices.dto.RouteResponseDto;
import routesservices.routesservices.repository.RouteRepository;

import java.util.List;

/**
 * BE-012: historial de rutas paginado (más reciente primero). La protección
 * por autenticación (RNF-03) se aplicará cuando AUTH-001 esté implementado;
 * este issue solo expone la consulta.
 */
@Service
public class RouteHistoryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final RouteRepository routeRepository;

    public RouteHistoryService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Transactional(readOnly = true)
    public List<RouteResponseDto> getHistory(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page debe ser >= 0");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size debe estar entre 1 y " + MAX_PAGE_SIZE);
        }

        return routeRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(RouteResponseMapper::toRouteResponse)
                .getContent();
    }
}
