package routesservices.routesservices.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import routesservices.routesservices.entity.DeliveryPoint;

import java.util.UUID;

public interface DeliveryPointRepository extends JpaRepository<DeliveryPoint, UUID> {
}
