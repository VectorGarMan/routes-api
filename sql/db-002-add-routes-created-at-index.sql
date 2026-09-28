-- DB-002: índice para consultar el historial de rutas ordenado por fecha.
-- Ejecutar contra una base de datos que ya tenga el esquema de schema.sql
-- aplicado (schema.sql ya incluye este índice para instalaciones nuevas).
CREATE INDEX IF NOT EXISTS idx_routes_created_at ON routes (created_at DESC);
