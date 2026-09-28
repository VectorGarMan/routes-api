package routesservices.routesservices.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import routesservices.routesservices.dto.ApiError;
import routesservices.routesservices.dto.ApiResponse;
import routesservices.routesservices.dto.ErrorCode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo unificado de errores de captura y Maps (BE-004).
 * code es estable para el frontend; message puede cambiar sin romper integraciones.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LocationInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocationInvalid(LocationInvalidException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage(), new ApiError(ErrorCode.LOCATION_INVALID.name(), null)));
    }

    @ExceptionHandler(MapsUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMapsUnavailable(MapsUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(ex.getMessage(), new ApiError(ErrorCode.MAPS_UNAVAILABLE.name(), null)));
    }

    @ExceptionHandler(MapsRateLimitException.class)
    public ResponseEntity<ApiResponse<Void>> handleMapsRateLimit(MapsRateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.error(ex.getMessage(), new ApiError(ErrorCode.MAPS_RATE_LIMIT.name(), null)));
    }

    @ExceptionHandler(RouteInfeasibleException.class)
    public ResponseEntity<ApiResponse<Void>> handleRouteInfeasible(RouteInfeasibleException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage(), new ApiError(ErrorCode.ROUTE_INFEASIBLE.name(), null)));
    }

    @ExceptionHandler(OptimizerUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimizerUnavailable(OptimizerUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(ex.getMessage(), new ApiError(ErrorCode.OPTIMIZER_UNAVAILABLE.name(), null)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fieldError -> fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Datos inválidos", new ApiError(ErrorCode.VALIDATION_ERROR.name(), fieldErrors)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(), new ApiError(ErrorCode.VALIDATION_ERROR.name(), null)));
    }
}
