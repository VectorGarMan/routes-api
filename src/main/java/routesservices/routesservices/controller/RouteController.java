package routesservices.routesservices.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import routesservices.routesservices.dto.ApiResponse;
import routesservices.routesservices.dto.OptimizeRouteRequest;
import routesservices.routesservices.dto.RouteResponseDto;
import routesservices.routesservices.service.RouteOptimizationService;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteOptimizationService service;

    public RouteController(RouteOptimizationService service) {
        this.service = service;
    }

    @PostMapping("/optimize")
    public ResponseEntity<ApiResponse<RouteResponseDto>> optimize(@Valid @RequestBody OptimizeRouteRequest request) {
        RouteResponseDto response = service.optimize(request);
        return ResponseEntity.ok(ApiResponse.ok("Ruta calculada", response));
    }
}
