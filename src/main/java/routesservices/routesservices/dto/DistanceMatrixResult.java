package routesservices.routesservices.dto;

import java.math.BigDecimal;

/**
 * distanceMatrix[i][j] / timeMatrix[i][j] = distancia/tiempo desde points[i] hasta points[j].
 * N = points.length; ambas matrices deben ser NxN (global_data_contract).
 */
public record DistanceMatrixResult(BigDecimal[][] distanceMatrix, BigDecimal[][] timeMatrix) {
}
