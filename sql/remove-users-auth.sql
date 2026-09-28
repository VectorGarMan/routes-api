-- Elimina todo lo relacionado con usuarios/autenticación (decisión de
-- negocio: acceso libre). Ejecutar contra una base de datos que ya tenga
-- el esquema de schema.sql aplicado con la tabla users y las columnas
-- created_by (versión previa a este cambio).

DROP INDEX IF EXISTS idx_routes_created_by;
DROP INDEX IF EXISTS idx_delivery_points_created_by;

ALTER TABLE routes DROP COLUMN IF EXISTS created_by;
ALTER TABLE delivery_points DROP COLUMN IF EXISTS created_by;

DROP TABLE IF EXISTS users;
