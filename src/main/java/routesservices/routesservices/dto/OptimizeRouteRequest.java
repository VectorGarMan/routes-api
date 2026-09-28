package routesservices.routesservices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record OptimizeRouteRequest(
        @NotEmpty List<@NotBlank String> pointIds,
        @NotBlank @Pattern(regexp = "DISTANCE|TIME") String objective,
        @NotBlank String depotPointId
) {
}
