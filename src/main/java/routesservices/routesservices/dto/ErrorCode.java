package routesservices.routesservices.dto;

/**
 * Códigos de error estables del contrato (BE-004). El texto de {@code message}
 * puede cambiar libremente; React decide qué mostrar únicamente a partir de {@code code}.
 */
public enum ErrorCode {
    LOCATION_INVALID,
    MAPS_UNAVAILABLE,
    MAPS_RATE_LIMIT,
    VALIDATION_ERROR
}
