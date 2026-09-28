-- DB-001: Esquema inicial de base de datos
-- Tablas: users, delivery_points, routes, route_stops
-- Los IDs de integracion (contrato React/Spring/Python) son UUID, nunca IDs numericos.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE delivery_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    latitude NUMERIC(9,6) NOT NULL,
    longitude NUMERIC(9,6) NOT NULL,
    time_window_start TIMESTAMPTZ,
    time_window_end TIMESTAMPTZ,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_delivery_points_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_delivery_points_longitude CHECK (longitude BETWEEN -180 AND 180)
);

CREATE TABLE routes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    objective VARCHAR(20) NOT NULL,
    depot_point_id UUID NOT NULL REFERENCES delivery_points(id),
    total_distance_meters NUMERIC(12,2),
    total_time_seconds NUMERIC(12,2),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_routes_request_id UNIQUE (request_id),
    CONSTRAINT chk_routes_status CHECK (status IN ('CALCULATING', 'ACTIVE', 'COMPLETED', 'ERROR')),
    CONSTRAINT chk_routes_objective CHECK (objective IN ('DISTANCE', 'TIME'))
);

CREATE TABLE route_stops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id UUID NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    point_id UUID NOT NULL REFERENCES delivery_points(id),
    stop_order INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    visited_at TIMESTAMPTZ,
    -- BE-011: tiempo estimado (segundos) del tramo parada_anterior -> esta parada,
    -- calculado al momento de crear/recalcular la ruta. Se compara luego contra el
    -- tiempo actual para detectar cambios significativos de tráfico.
    estimated_leg_seconds NUMERIC(12,2),
    CONSTRAINT chk_route_stops_status CHECK (status IN ('PENDING', 'CURRENT', 'VISITED')),
    CONSTRAINT uq_route_stops_order UNIQUE (route_id, stop_order),
    CONSTRAINT uq_route_stops_point UNIQUE (route_id, point_id)
);

CREATE INDEX idx_route_stops_route_id ON route_stops (route_id);
CREATE INDEX idx_routes_created_by ON routes (created_by);
CREATE INDEX idx_delivery_points_created_by ON delivery_points (created_by);
