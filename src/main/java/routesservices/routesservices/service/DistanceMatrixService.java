package routesservices.routesservices.service;

import org.springframework.stereotype.Service;
import routesservices.routesservices.client.DistanceTimeResult;
import routesservices.routesservices.dto.DistanceMatrixResult;
import routesservices.routesservices.entity.DeliveryPoint;

import java.math.BigDecimal;
import java.util.List;

/**
 * Construye distanceMatrix y timeMatrix (NxN) manteniendo exactamente el orden
 * de points recibido durante toda la solicitud. [i][j] = points[i] -> points[j].
 */
@Service
public class DistanceMatrixService {

    private final PointDistanceService pointDistanceService;

    public DistanceMatrixService(PointDistanceService pointDistanceService) {
        this.pointDistanceService = pointDistanceService;
    }

    public DistanceMatrixResult buildMatrix(List<DeliveryPoint> points) {
        int n = points.size();
        BigDecimal[][] distanceMatrix = new BigDecimal[n][n];
        BigDecimal[][] timeMatrix = new BigDecimal[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                DistanceTimeResult result = pointDistanceService.getDistanceAndTime(points.get(i), points.get(j));
                distanceMatrix[i][j] = result.distanceMeters();
                timeMatrix[i][j] = result.durationSeconds();
            }
        }

        validateSquare(distanceMatrix, n);
        validateSquare(timeMatrix, n);

        return new DistanceMatrixResult(distanceMatrix, timeMatrix);
    }

    private void validateSquare(BigDecimal[][] matrix, int n) {
        if (matrix.length != n) {
            throw new IllegalStateException("La matriz no tiene N filas (N=" + n + ")");
        }
        for (BigDecimal[] row : matrix) {
            if (row.length != n) {
                throw new IllegalStateException("La matriz no es NxN (N=" + n + ")");
            }
        }
    }
}
