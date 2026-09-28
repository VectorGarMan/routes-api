package routesservices.routesservices.exception;

public class OptimizerUnavailableException extends RuntimeException {

    public OptimizerUnavailableException(String message) {
        super(message);
    }

    public OptimizerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
