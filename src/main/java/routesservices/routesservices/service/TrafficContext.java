package routesservices.routesservices.service;

/**
 * Contexto de tráfico usado como dimensión de la caché (BE-007) y para decidir
 * cómo se obtiene el tiempo de viaje (BE-010).
 */
public enum TrafficContext {
    /** Sin tráfico: perfil "driving" de Mapbox. */
    NONE,
    /** Tráfico en tiempo real: perfil "driving-traffic" de Mapbox. */
    REAL_TIME,
    /** Datos históricos/simulados, sin llamar a Mapbox (pruebas, evitar cargos duplicados). */
    SIMULATED
}
