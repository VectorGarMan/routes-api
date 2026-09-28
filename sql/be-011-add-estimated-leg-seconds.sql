-- BE-011: agrega la columna necesaria para comparar el tiempo estimado
-- anterior vs actual por tramo. Ejecutar contra una base de datos que ya
-- tenga el esquema de schema.sql aplicado (schema.sql ya incluye esta
-- columna para instalaciones nuevas desde cero).
ALTER TABLE route_stops ADD COLUMN IF NOT EXISTS estimated_leg_seconds NUMERIC(12,2);
