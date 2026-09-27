package routesservices.routesservices.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import routesservices.routesservices.exception.OptimizerUnavailableException;

@Component
public class PythonOptimizerClient {

    private final RestClient restClient;

    public PythonOptimizerClient(RestClient.Builder builder,
                                  @Value("${optimizer.python.base-url}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public OptimizeResponsePayload optimize(OptimizeRequestPayload request) {
        try {
            return restClient.post()
                    .uri("/optimize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OptimizeResponsePayload.class);
        } catch (RestClientException ex) {
            throw new OptimizerUnavailableException("El servicio de optimización no está disponible", ex);
        }
    }
}
