package routesservices.routesservices.exception;

public class MapsUnavailableException extends RuntimeException {

    public MapsUnavailableException(String message) {
        super(message);
    }

    public MapsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
