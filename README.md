<img width="1280" height="640" alt="3" src="https://github.com/user-attachments/assets/ba21ba58-d96b-46a1-9408-566f8d2e85ba" />

# Routes API — Optimización de rutas (Corredor Industrial El Salto)

Backend (Spring Boot) del proyecto de optimización de rutas para logística en el Corredor Industrial El Salto. Registra puntos de entrega, calcula la ruta óptima, le da seguimiento a la ruta (paradas visitadas y recálculo por tráfico) y guarda un historial.

Es de **acceso libre**: no hay login, usuarios ni autenticación (decisión de negocio).

## Cómo encaja con el resto del sistema

```
routes-client (React)  ──HTTP──►  routes-api (este repo)  ──►  Mapbox (geocodificación, distancias, tráfico, geometría)
                                        │                  ──►  PostgreSQL
                                        └─────────────────►  routes-optimizer-microservice (Python, POST /optimize)
```

| Repositorio | Rol |
|---|---|
| [`routes-client`](https://github.com/VectorGarMan/routes-client) | Frontend React. Solo habla con esta API. |
| `routes-api` (este) | Orquestador: valida, consulta Mapbox, persiste y coordina al optimizador. |
| [`routes-optimizer-microservice`](https://github.com/VectorGarMan/routes-optimizer-microservice) | FastAPI + OR-Tools. Recibe matrices ya calculadas y devuelve el orden de visita. |

Reglas del contrato: React nunca llama a Python ni a Mapbox directamente, y Python nunca llama a Mapbox (recibe las matrices ya construidas por Spring). Mapbox y su token viven **solo** aquí.

## Requisitos

- Java 21
- PostgreSQL (se puede levantar con Docker, ver abajo)
- El servicio Python del optimizador corriendo (por defecto en `http://localhost:8000`)
- Un token de Mapbox

## Variables de entorno

Nada sensible va en el repositorio: todo se lee del entorno (ver `src/main/resources/application.properties`).

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `MAPBOX_ACCESS_TOKEN` | Token de Mapbox. **Obligatorio** para registrar puntos, calcular distancias y obtener la geometría | *(vacío)* |
| `MAPBOX_BASE_URL` | URL base de Mapbox | `https://api.mapbox.com` |
| `OPTIMIZER_SERVICE_URL` | URL del servicio Python | `http://localhost:8000` |
| `DB_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/routesservices` |
| `DB_USERNAME` | Usuario de la base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña de la base de datos | `postgres` |

Los valores de base de datos por defecto son solo para desarrollo local.

## Puesta en marcha

### 1. Base de datos

```bash
docker run -d --name routesservices-postgres \
  -e POSTGRES_DB=routesservices -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:16

# Crear el esquema (bash / cmd)
docker exec -i -e PGPASSWORD=postgres routesservices-postgres \
  psql -U postgres -d routesservices < sql/schema.sql
```

En PowerShell, la redirección `<` no funciona; usa:

```powershell
Get-Content sql/schema.sql | docker exec -i -e PGPASSWORD=postgres routesservices-postgres psql -U postgres -d routesservices
```

El esquema **no** se gestiona con una herramienta de migraciones (`spring.jpa.hibernate.ddl-auto=none`): `sql/schema.sql` es la única fuente de verdad y está pensado para una base vacía.

> **Si tu base ya existía antes de la columna `routes.route_geometry`**, agrégala a mano:
> ```sql
> ALTER TABLE routes ADD COLUMN IF NOT EXISTS route_geometry TEXT;
> ```
> Sin ella, calcular o consultar rutas falla porque PostgreSQL no encuentra la columna.

### 2. Arrancar la API

```bash
# bash
MAPBOX_ACCESS_TOKEN=tu_token ./mvnw spring-boot:run
```

```powershell
# PowerShell
$env:MAPBOX_ACCESS_TOKEN = "tu_token"; ./mvnw spring-boot:run
```

La API queda en `http://localhost:8080`. Arranca también el optimizador Python (`uvicorn main:app` en `routes-optimizer-microservice`).

### 3. Documentación interactiva

La API usa springdoc-openapi, por lo que con la configuración por defecto de springdoc tienes Swagger UI en `http://localhost:8080/swagger-ui.html` y la especificación en `http://localhost:8080/v3/api-docs`. También hay una colección de Insomnia en `src/main/resources/Insomnia_2026-09-27.yaml`.

## Endpoints

Todas las respuestas usan el mismo envoltorio: `{ "success": true|false, "message": "...", "data": ..., "error": { "code": "...", "details": ... } | null }`.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/v1/delivery-points` | Registra y valida un punto de entrega con Mapbox (`address` o `latitude`/`longitude`) |
| `POST` | `/api/v1/routes/optimize` | Calcula la ruta óptima para puntos ya registrados |
| `POST` | `/api/v1/routes/{routeId}/recalculate` | Verifica el tráfico y recalcula solo si hubo un cambio significativo (sin body) |
| `POST` | `/api/v1/routes/{routeId}/stops/{pointId}/visit` | Marca una parada como visitada (sin body) |
| `GET` | `/api/v1/routes/history?page=0&size=20` | Historial de rutas, la más reciente primero |

Ejemplo de flujo mínimo:

```jsonc
// POST /api/v1/delivery-points  (repite para cada punto y guarda el "id" que devuelve)
{
  "reference": "Tienda Norte #42",
  "address": "Av. Vallarta 1234, Guadalajara, Jalisco",
  "timeWindow": { "start": "2026-10-01T08:00:00-06:00", "end": "2026-10-01T17:00:00-06:00" }
}

// POST /api/v1/routes/optimize
{
  "pointIds": ["<id-1>", "<id-2>", "<id-3>"],
  "objective": "DISTANCE",          // o "TIME"
  "depotPointId": "<id-1>"          // debe estar incluido en pointIds
}
```

La respuesta de una ruta (`RouteResponse`) incluye `routeId`, `status`, `stops` (`pointId`, `order`, `status`), `totalDistanceMeters`, `totalTimeSeconds`, `routeGeometry` y `updatedAt`. Distancias en **metros** y tiempos en **segundos**; la conversión a km/minutos es solo de presentación.

### Comportamiento a tener en cuenta

- **Ruta de ida y vuelta.** El depósito es punto de partida y de regreso: `totalDistanceMeters` y `totalTimeSeconds` incluyen el tramo de vuelta al depósito, aunque `stops` no liste una parada final de regreso.
- **Tráfico real.** El cálculo usa el perfil `driving-traffic` de Mapbox; solo afecta a los tiempos, no a las distancias.
- **Geometría real (`routeGeometry`).** Al calcular o recalcular, la API pide a Mapbox Directions el recorrido por calles y lo guarda como lista de `[longitud, latitud]` (GeoJSON) para que el cliente lo dibuje. Es `null` si Mapbox no pudo devolverla (p. ej. si rechaza la solicitud por exceso de paradas; Mapbox admite hasta 25 puntos) o si la ruta es anterior a este cambio; en ese caso la ruta se guarda igual.
- **Recálculo.** Compara, tramo por tramo de las paradas pendientes, el tiempo guardado contra uno nuevo con tráfico real. Si algún tramo cambia más de **25 %**, recalcula excluyendo las paradas ya visitadas; si no, devuelve la ruta sin modificar. El cliente lo invoca por *polling*.
- **Paradas visitadas.** Deben marcarse en orden (no se puede saltar una pendiente anterior), marcar una ya visitada es idempotente, y al marcar la última la ruta pasa a `COMPLETED`.
- **Estados.** Ruta: `CALCULATING`, `ACTIVE`, `COMPLETED`, `ERROR` (en el código actual se asignan `ACTIVE` al calcular y `COMPLETED` al terminar). Parada: `PENDING`, `VISITED`.
- **CORS.** Solo se permite el origen `http://localhost:5173` (Vite) sobre `/api/**`.

### Códigos de error

El campo `error.code` es estable; el texto de `message` puede cambiar.

| Código | HTTP | Cuándo |
|---|---|---|
| `LOCATION_INVALID` | 422 | Mapbox no reconoce la ubicación del punto |
| `VALIDATION_ERROR` | 400 | Datos inválidos, ids inexistentes o parada marcada fuera de orden (el `message` explica cuál) |
| `MAPS_UNAVAILABLE` | 503 | Mapbox no responde |
| `MAPS_RATE_LIMIT` | 429 | Mapbox limitó las solicitudes |
| `ROUTE_INFEASIBLE` | 422 | El optimizador no encontró una ruta viable |
| `OPTIMIZER_UNAVAILABLE` | 503 | El servicio Python no responde o devolvió un estado inesperado |

## Pruebas

```bash
# Pruebas unitarias (no necesitan base de datos ni Mapbox)
./mvnw -Dtest='*ServiceTest,SimulatedTrafficProviderTest' test

# Todas (incluye RoutesservicesApplicationTests, que arranca el contexto y requiere PostgreSQL)
./mvnw test
```

Para probar tráfico sin consumir Mapbox existe `TrafficContext.SIMULATED` (distancia por haversine con un factor de congestión fijo), pensado para pruebas.

## Estructura del proyecto

```
src/main/java/routesservices/routesservices/
├── controller/   # Endpoints REST (RouteController, DeliveryPointController)
├── service/      # Lógica: cálculo, recálculo, historial, paradas, matrices, caché
├── client/       # Clientes HTTP: Mapbox (geocodificación y Directions) y optimizador Python
├── repository/   # Spring Data JPA
├── entity/       # DeliveryPoint, Route, RouteStop
├── dto/          # Contrato JSON (ApiResponse, RouteResponseDto, ErrorCode...)
├── exception/    # Excepciones y GlobalExceptionHandler
└── config/       # WebConfig (CORS) y beans de cliente HTTP
sql/schema.sql    # Esquema maestro de PostgreSQL
docs/             # Diseño de la base de datos (ERD y diccionario de datos)
```

El backlog del proyecto está en `src/main/resources/backlog_issues_optimizacion_rutas_el_salto.json`. El ERD de `docs/db-001-esquema-base-datos.md` es de la versión inicial del esquema; el esquema vigente es `sql/schema.sql`.

## Limitaciones conocidas

- `GET /history` devuelve la `routeGeometry` de cada ruta; con muchas rutas por página la respuesta puede ser pesada.
- Las anotaciones de Swagger indican 404 para ids inexistentes, pero la API responde 400 `VALIDATION_ERROR`.
- El origen permitido por CORS está fijo en `WebConfig`.
