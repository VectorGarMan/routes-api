from typing import List, Optional
from pydantic import BaseModel, Field, model_validator

# --- SUBMODELOS ---
class TimeWindow(BaseModel):
    start: Optional[str] = None
    end: Optional[str] = None

class DeliveryPoint(BaseModel):
    id: str
    reference: str
    address: Optional[str] = None
    latitude: float
    longitude: float
    timeWindow: Optional[TimeWindow] = None

class TimeWindowSeconds(BaseModel):
    startSecond: int
    endSecond: int

# --- MODELO DE ENTRADA (REQUEST) ---
class OptimizeRequest(BaseModel):
    requestId: str
    points: List[DeliveryPoint]
    distanceMatrix: List[List[float]]
    timeMatrix: List[List[float]]
    objective: str = Field(..., pattern="^(DISTANCE|TIME)$") # Solo permite estos dos valores
    depotIndex: int
    timeWindows: Optional[List[TimeWindowSeconds]] = None

    # VALIDACIÓN: Rechazar matrices cuyo tamaño no coincida con los puntos
    @model_validator(mode='after')
    def check_matrices_size(self):
        n = len(self.points)
        
        # Comprobar que distanceMatrix sea NxN
        if len(self.distanceMatrix) != n or any(len(row) != n for row in self.distanceMatrix):
            raise ValueError(f"La matriz distanceMatrix debe ser exactamente de {n}x{n} para coincidir con la cantidad de points.")
            
        # Comprobar que timeMatrix sea NxN
        if len(self.timeMatrix) != n or any(len(row) != n for row in self.timeMatrix):
            raise ValueError(f"La matriz timeMatrix debe ser exactamente de {n}x{n} para coincidir con la cantidad de points.")
            
        return self

# --- MODELO DE SALIDA (RESPONSE) ---
class OptimizeResponse(BaseModel):
    requestId: str
    route: List[str]
    routeIndexes: List[int]
    totalDistanceMeters: float
    totalTimeSeconds: float
    status: str = Field(..., pattern="^(SUCCESS|INFEASIBLE|ERROR)$")
    errorCode: Optional[str] = None
    message: Optional[str] = None