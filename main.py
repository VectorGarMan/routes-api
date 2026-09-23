from fastapi import FastAPI, HTTPException
from models import OptimizeRequest, OptimizeResponse
from solver import calculate_optimal_route # <- Importamos tu nuevo motor matemático

app = FastAPI(
    title="Optimizador de Rutas - El Salto",
    description="Servicio Python para calcular rutas óptimas del corredor industrial",
    version="1.0.0"
)

@app.get("/health")
def health_check():
    return {"status": "ok", "message": "El servicio del optimizador está funcionando correctamente."}

@app.post("/optimize", response_model=OptimizeResponse)
def optimize_route(request: OptimizeRequest):
    # 1. Llamamos a OR-Tools pasándole los datos del Backend
    result = calculate_optimal_route(request)
    
    # 2. Si el motor indica que es matemáticamente imposible
    if result["status"] == "INFEASIBLE":
        return OptimizeResponse(
            requestId=request.requestId,
            route=[],
            routeIndexes=[],
            totalDistanceMeters=0.0,
            totalTimeSeconds=0.0,
            status="INFEASIBLE",
            errorCode="ROUTE_INFEASIBLE",
            message="No se encontró una ruta viable (revisar ventanas de tiempo)."
        )
        
    # 3. Si fue exitoso, mapeamos los identificadores
    # Convertimos los índices matemáticos de OR-Tools a los IDs UUID reales de Spring Boot
    ordered_ids = [request.points[i].id for i in result["route_indexes"]]

    return OptimizeResponse(
        requestId=request.requestId,
        route=ordered_ids,
        routeIndexes=result["route_indexes"],
        totalDistanceMeters=result["total_distance"],
        totalTimeSeconds=result["total_time"],
        status="SUCCESS"
    )