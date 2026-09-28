package routesservices.routesservices.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import routesservices.routesservices.dto.OptimizeRouteRequest;
import routesservices.routesservices.dto.RouteResponseDto;
import routesservices.routesservices.service.RouteHistoryService;
import routesservices.routesservices.service.RouteOptimizationService;
import routesservices.routesservices.service.RouteRecalculationService;
import routesservices.routesservices.service.RouteStopService;

import java.util.List;
import java.util.UUID;

@Tag(name = "Rutas", description = "Optimización, seguimiento e historial de rutas de entrega")
@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteOptimizationService service;
    private final RouteRecalculationService recalculationService;
    private final RouteHistoryService historyService;
    private final RouteStopService stopService;

    public RouteController(RouteOptimizationService service, RouteRecalculationService recalculationService,
                           RouteHistoryService historyService, RouteStopService stopService) {
        this.service = service;
        this.recalculationService = recalculationService;
        this.historyService = historyService;
        this.stopService = stopService;
    }

    @Operation(
            summary = "Calcular ruta óptima",
            description = "Recibe una lista de puntos de entrega ya registrados y calcula el orden de visita " +
                          "más eficiente según el criterio elegido (distancia o tiempo). " +
                          "El depósito es el punto de partida y de retorno.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OptimizeRouteRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Optimizar por distancia",
                                            value = """
                                                    {
                                                      "pointIds": [
                                                        "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                        "b2c3d4e5-f6a7-8901-bcde-f12345678901",
                                                        "c3d4e5f6-a7b8-9012-cdef-123456789012"
                                                      ],
                                                      "objective": "DISTANCE",
                                                      "depotPointId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                                                    }"""
                                    ),
                                    @ExampleObject(
                                            name = "Optimizar por tiempo",
                                            value = """
                                                    {
                                                      "pointIds": [
                                                        "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                        "b2c3d4e5-f6a7-8901-bcde-f12345678901",
                                                        "c3d4e5f6-a7b8-9012-cdef-123456789012"
                                                      ],
                                                      "objective": "TIME",
                                                      "depotPointId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                                                    }"""
                                    )
                            }
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ruta calculada correctamente"),
                    @ApiResponse(responseCode = "400", description = "Payload inválido o IDs desconocidos"),
                    @ApiResponse(responseCode = "503", description = "Servicio de optimización o de mapas no disponible")
            }
    )
    @PostMapping("/optimize")
    public ResponseEntity<routesservices.routesservices.dto.ApiResponse<RouteResponseDto>> optimize(
            @Valid @org.springframework.web.bind.annotation.RequestBody OptimizeRouteRequest request) {
        RouteResponseDto response = service.optimize(request);
        return ResponseEntity.ok(routesservices.routesservices.dto.ApiResponse.ok("Ruta calculada", response));
    }

    @Operation(
            summary = "Recalcular ruta si hay cambios de tráfico",
            description = "Llámalo periódicamente (polling) durante la ejecución de la ruta. " +
                          "Si ningún tramo cambió de forma significativa, devuelve la ruta sin modificarla. " +
                          "Útil para detectar atascos o cierres viales en tiempo real.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ruta verificada (puede incluir reordenamiento)"),
                    @ApiResponse(responseCode = "404", description = "Ruta no encontrada")
            }
    )
    @PostMapping("/{routeId}/recalculate")
    public ResponseEntity<routesservices.routesservices.dto.ApiResponse<RouteResponseDto>> recalculate(
            @Parameter(description = "ID de la ruta activa", example = "d4e5f6a7-b8c9-0123-defa-456789012345")
            @PathVariable UUID routeId) {
        RouteResponseDto response = recalculationService.recalculateIfNeeded(routeId);
        return ResponseEntity.ok(routesservices.routesservices.dto.ApiResponse.ok("Ruta verificada", response));
    }

    @Operation(
            summary = "Historial de rutas",
            description = "Devuelve las rutas generadas ordenadas de más reciente a más antigua. " +
                          "Soporta paginación con los parámetros page y size.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de rutas paginada")
            }
    )
    @GetMapping("/history")
    public ResponseEntity<routesservices.routesservices.dto.ApiResponse<List<RouteResponseDto>>> history(
            @Parameter(description = "Número de página (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Cantidad de resultados por página", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        List<RouteResponseDto> history = historyService.getHistory(page, size);
        return ResponseEntity.ok(routesservices.routesservices.dto.ApiResponse.ok("Historial de rutas", history));
    }

    @Operation(
            summary = "Marcar parada como visitada",
            description = "Registra que el repartidor llegó al punto indicado. " +
                          "Las paradas deben marcarse en orden secuencial; " +
                          "marcar una ya visitada es idempotente. " +
                          "Cuando se visita la última parada pendiente, la ruta pasa a estado COMPLETED.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Parada marcada correctamente"),
                    @ApiResponse(responseCode = "400", description = "Se intentó saltar una parada pendiente anterior"),
                    @ApiResponse(responseCode = "404", description = "Ruta o parada no encontrada")
            }
    )
    @PostMapping("/{routeId}/stops/{pointId}/visit")
    public ResponseEntity<routesservices.routesservices.dto.ApiResponse<RouteResponseDto>> markStopVisited(
            @Parameter(description = "ID de la ruta", example = "d4e5f6a7-b8c9-0123-defa-456789012345")
            @PathVariable UUID routeId,
            @Parameter(description = "ID del punto de entrega a marcar como visitado", example = "b2c3d4e5-f6a7-8901-bcde-f12345678901")
            @PathVariable UUID pointId) {
        RouteResponseDto response = stopService.markVisited(routeId, pointId);
        return ResponseEntity.ok(routesservices.routesservices.dto.ApiResponse.ok("Parada marcada como visitada", response));
    }
}
