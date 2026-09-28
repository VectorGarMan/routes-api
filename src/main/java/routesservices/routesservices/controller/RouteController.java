package routesservices.routesservices.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import routesservices.routesservices.dto.ApiResponse;
import routesservices.routesservices.dto.OptimizeRouteRequest;
import routesservices.routesservices.dto.RouteResponseDto;
import routesservices.routesservices.service.RouteHistoryService;
import routesservices.routesservices.service.RouteOptimizationService;
import routesservices.routesservices.service.RouteRecalculationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteOptimizationService service;
    private final RouteRecalculationService recalculationService;
    private final RouteHistoryService historyService;

    public RouteController(RouteOptimizationService service, RouteRecalculationService recalculationService,
                            RouteHistoryService historyService) {
        this.service = service;
        this.recalculationService = recalculationService;
        this.historyService = historyService;
    }

    @PostMapping("/optimize")
    public ResponseEntity<ApiResponse<RouteResponseDto>> optimize(@Valid @RequestBody OptimizeRouteRequest request) {
        RouteResponseDto response = service.optimize(request);
        return ResponseEntity.ok(ApiResponse.ok("Ruta calculada", response));
    }

    /**
     * BE-011: el frontend lo llama periódicamente (polling). Si ningún tramo
     * pendiente cambió de forma significativa, devuelve la ruta sin modificar.
     */
    @PostMapping("/{routeId}/recalculate")
    public ResponseEntity<ApiResponse<RouteResponseDto>> recalculate(@PathVariable UUID routeId) {
        RouteResponseDto response = recalculationService.recalculateIfNeeded(routeId);
        return ResponseEntity.ok(ApiResponse.ok("Ruta verificada", response));
    }

    /** BE-012: paginado (más reciente primero); sin autenticación todavía (pendiente de AUTH-001). */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<RouteResponseDto>>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<RouteResponseDto> history = historyService.getHistory(page, size);
        return ResponseEntity.ok(ApiResponse.ok("Historial de rutas", history));
    }
}
