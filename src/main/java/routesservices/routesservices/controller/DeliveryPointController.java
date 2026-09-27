package routesservices.routesservices.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import routesservices.routesservices.dto.ApiResponse;
import routesservices.routesservices.dto.DeliveryPointRequest;
import routesservices.routesservices.dto.DeliveryPointResponse;
import routesservices.routesservices.service.DeliveryPointService;

@RestController
@RequestMapping("/api/v1/delivery-points")
public class DeliveryPointController {

    private final DeliveryPointService service;

    public DeliveryPointController(DeliveryPointService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryPointResponse>> create(@Valid @RequestBody DeliveryPointRequest request) {
        DeliveryPointResponse response = service.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Punto de entrega registrado", response));
    }
}
