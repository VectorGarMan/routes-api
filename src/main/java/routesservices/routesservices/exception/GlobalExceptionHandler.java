package routesservices.routesservices.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import routesservices.routesservices.dto.ApiError;
import routesservices.routesservices.dto.ApiResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo mínimo de errores para BE-003 (LOCATION_INVALID, VALIDATION_ERROR).
 * BE-004 unifica aquí los códigos restantes (MAPS_UNAVAILABLE, MAPS_RATE_LIMIT).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LocationInvalidException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocationInvalid(LocationInvalidException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage(), new ApiError("LOCATION_INVALID", null)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fieldError -> fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Datos inválidos", new ApiError("VALIDATION_ERROR", fieldErrors)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage(), new ApiError("VALIDATION_ERROR", null)));
    }
}
