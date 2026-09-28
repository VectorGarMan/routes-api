package routesservices.routesservices.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import routesservices.routesservices.entity.Route;

import java.util.UUID;

public interface RouteRepository extends JpaRepository<Route, UUID> {
}
