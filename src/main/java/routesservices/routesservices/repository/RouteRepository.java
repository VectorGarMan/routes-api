package routesservices.routesservices.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import routesservices.routesservices.entity.Route;

import java.util.UUID;

public interface RouteRepository extends JpaRepository<Route, UUID> {

    /** Historial de rutas (DB-002), más reciente primero; BE-012 la expone paginada. */
    Page<Route> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
