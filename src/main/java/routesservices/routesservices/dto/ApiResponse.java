package routesservices.routesservices.dto;

public record ApiResponse<T>(boolean success, String message, T data, ApiError error) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> error(String message, ApiError error) {
        return new ApiResponse<>(false, message, null, error);
    }
}
