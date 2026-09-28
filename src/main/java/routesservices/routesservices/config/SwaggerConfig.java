package routesservices.routesservices.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI routesServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Routes Optimization Service")
                        .description("""
                                API para registrar puntos de entrega y calcular rutas óptimas. \
                                Los endpoints cubren el ciclo completo: registro de puntos, \
                                optimización de ruta, seguimiento en tiempo real e historial.""")
                        .version("v1")
                        .contact(new Contact()
                                .name("Equipo El Salto")
                                .email("dev@elsalto.mx")));
    }
}
