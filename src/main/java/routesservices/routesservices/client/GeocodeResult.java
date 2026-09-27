package routesservices.routesservices.client;

import java.math.BigDecimal;

public record GeocodeResult(boolean valid, BigDecimal latitude, BigDecimal longitude, String formattedAddress) {

    public static GeocodeResult valid(BigDecimal latitude, BigDecimal longitude, String formattedAddress) {
        return new GeocodeResult(true, latitude, longitude, formattedAddress);
    }

    public static GeocodeResult invalid() {
        return new GeocodeResult(false, null, null, null);
    }
}
