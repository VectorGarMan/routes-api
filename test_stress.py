import requests
import random
import time

def ejecutar_prueba(num_puntos, objetivo="DISTANCE"):
    # 1. Generar puntos dinámicos
    puntos = [{"id": f"uuid-{i}", "reference": f"Punto {i}", "latitude": 20.5, "longitude": -103.3} for i in range(num_puntos)]
    
    # 2. Generar matrices NxN con valores aleatorios
    dist_matrix = [[random.randint(100, 2000) if i != j else 0 for j in range(num_puntos)] for i in range(num_puntos)]
    time_matrix = [[random.randint(60, 600) if i != j else 0 for j in range(num_puntos)] for i in range(num_puntos)]
    
    # 3. Armar el payload del contrato
    payload = {
        "requestId": f"prueba-{num_puntos}-{objetivo}",
        "points": puntos,
        "distanceMatrix": dist_matrix,
        "timeMatrix": time_matrix,
        "objective": objetivo,
        "depotIndex": 0
    }
    
    # 4. Medir tiempo de respuesta y hacer la petición
    inicio = time.time()
    respuesta = requests.post("http://127.0.0.1:8000/optimize", json=payload)
    fin = time.time()
    
    datos = respuesta.json()
    tiempo_total = fin - inicio
    
    print(f"Ruta de {num_puntos} puntos | Objetivo: {objetivo} | Status: {datos.get('status')} | Tiempo: {tiempo_total:.4f} seg")

# Ejecutar los escenarios exigidos por la tarea PY-005
escenarios = [1, 2, 5, 10, 20]

print("--- INICIANDO PRUEBAS DE ESTRÉS ---")
for n in escenarios:
    ejecutar_prueba(n, "DISTANCE")
    ejecutar_prueba(n, "TIME")