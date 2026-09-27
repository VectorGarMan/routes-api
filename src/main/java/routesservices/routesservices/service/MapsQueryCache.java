package routesservices.routesservices.service;

import routesservices.routesservices.client.DistanceTimeResult;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Caché de consultas a Maps (BE-007), con clave origen + destino + contexto de
 * tráfico. Vive únicamente durante una sesión de cálculo: no es un bean de
 * Spring ni persiste entre solicitudes — el llamador crea una instancia al
 * iniciar el cálculo y la descarta (deja de referenciarla) al terminarlo.
 */
public class MapsQueryCache {

    private record CacheKey(UUID originId, UUID destinationId, String trafficContext) {
    }

    private final Map<CacheKey, DistanceTimeResult> cache = new ConcurrentHashMap<>();

    public DistanceTimeResult getOrCompute(UUID originId, UUID destinationId, String trafficContext,
                                            Supplier<DistanceTimeResult> supplier) {
        CacheKey key = new CacheKey(originId, destinationId, Objects.requireNonNullElse(trafficContext, "DEFAULT"));
        return cache.computeIfAbsent(key, ignored -> supplier.get());
    }

    public void clear() {
        cache.clear();
    }
}
