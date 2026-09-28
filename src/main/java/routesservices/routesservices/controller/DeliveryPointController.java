package routesservices.routesservices.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import routesservices.routesservices.dto.DeliveryPointRequest;
import routesservices.routesservices.dto.DeliveryPointResponse;
import routesservices.routesservices.service.DeliveryPointService;

@Tag(name = "Puntos de entrega", description = "Registro y consulta de puntos de entrega")
@RestController
@RequestMapping("/api/v1/delivery-points")
public class DeliveryPointController {

    private final DeliveryPointService service;

    public DeliveryPointController(DeliveryPointService service) {
        this.service = service;
    }

    @Operation(
            summary = "Registrar un punto de entrega",
            description = "Crea un nuevo punto de entrega con sus coordenadas y ventana de tiempo opcional. " +
                          "El punto quedará disponible para incluirlo en futuras rutas.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DeliveryPointRequest.class),
                            examples = @ExampleObject(
                                    name = "Punto con ventana de tiempo",
                                    value = """
                                            {
                                              "reference": "Tienda Norte #42",
                                              "address": "Av. Vallarta 1234, Guadalajara, Jalisco",
                                              "latitude": 20.6597,
                                              "longitude": -103.3496,
                                              "timeWindow": {
                                                "start": "2025-06-10T08:00:00-06:00",
                                                "end": "2025-06-10T12:00:00-06:00"
                                              }
                                            }"""
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Punto de entrega creado correctamente"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Datos inválidos (coordenadas fuera de rango, referencia vacía, etc.)"
                    )
            }
    )
    @PostMapping
    public ResponseEntity<routesservices.routesservices.dto.ApiResponse<DeliveryPointResponse>> create(
            @Valid @org.springframework.web.bind.annotation.RequestBody DeliveryPointRequest request) {
        DeliveryPointResponse response = service.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(routesservices.routesservices.dto.ApiResponse.ok("Punto de entrega registrado", response));
    }
}
