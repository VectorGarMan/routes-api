package routesservices.routesservices.service;

import org.springframework.stereotype.Service;
import routesservices.routesservices.client.GeocodeResult;
import routesservices.routesservices.client.MapboxClient;
import routesservices.routesservices.dto.DeliveryPointRequest;
import routesservices.routesservices.dto.DeliveryPointResponse;
import routesservices.routesservices.dto.TimeWindowDto;
import routesservices.routesservices.entity.DeliveryPoint;
import routesservices.routesservices.entity.TimeWindow;
import routesservices.routesservices.exception.LocationInvalidException;
import routesservices.routesservices.repository.DeliveryPointRepository;

@Service
public class DeliveryPointService {

    private final DeliveryPointRepository repository;
    private final MapboxClient mapboxClient;

    public DeliveryPointService(DeliveryPointRepository repository, MapboxClient mapboxClient) {
        this.repository = repository;
        this.mapboxClient = mapboxClient;
    }

    public DeliveryPointResponse register(DeliveryPointRequest request) {
        boolean hasCoordinates = request.latitude() != null && request.longitude() != null;
        boolean hasAddress = request.address() != null && !request.address().isBlank();

        if (!hasCoordinates && !hasAddress) {
            throw new IllegalArgumentException("Se requiere address o latitude/longitude");
        }

        GeocodeResult geocodeResult = hasAddress
                ? mapboxClient.geocode(request.address())
                : mapboxClient.reverseGeocode(request.latitude(), request.longitude());

        if (!geocodeResult.valid()) {
            throw new LocationInvalidException("La ubicación no pudo ser validada");
        }

        DeliveryPoint point = DeliveryPoint.builder()
                .reference(request.reference())
                .address(geocodeResult.formattedAddress() != null ? geocodeResult.formattedAddress() : request.address())
                .latitude(hasCoordinates ? request.latitude() : geocodeResult.latitude())
                .longitude(hasCoordinates ? request.longitude() : geocodeResult.longitude())
                .timeWindow(toEntityTimeWindow(request.timeWindow()))
                .build();

        DeliveryPoint saved = repository.save(point);
        return DeliveryPointResponse.from(saved);
    }

    private TimeWindow toEntityTimeWindow(TimeWindowDto dto) {
        if (dto == null) {
            return null;
        }
        return new TimeWindow(dto.start(), dto.end());
    }
}
