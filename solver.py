from ortools.constraint_solver import routing_enums_pb2
from ortools.constraint_solver import pywrapcp
from models import OptimizeRequest

def calculate_optimal_route(request: OptimizeRequest):
    # 1. Preparar las matrices
    # OR-Tools requiere que los costos sean números enteros. 
    # Como el backend envía metros y segundos (que ya son precisos), los convertimos a enteros de forma segura.
    distance_matrix = [[int(val) for val in row] for row in request.distanceMatrix]
    time_matrix = [[int(val) for val in row] for row in request.timeMatrix]

    # Definir qué matriz será nuestro "Costo" a minimizar según el objetivo
    if request.objective == "DISTANCE":
        cost_matrix = distance_matrix
    else:
        cost_matrix = time_matrix

    # 2. Inicializar el administrador de rutas de OR-Tools
    # Parámetros: (número_de_puntos, número_de_vehículos, índice_del_depósito)
    manager = pywrapcp.RoutingIndexManager(len(request.points), 1, request.depotIndex)
    routing = pywrapcp.RoutingModel(manager)

    # 3. Crear el Callback de Costo
    # Esta función le dice a OR-Tools cuánto cuesta viajar del punto A al punto B
    def cost_callback(from_index, to_index):
        from_node = manager.IndexToNode(from_index)
        to_node = manager.IndexToNode(to_index)
        return cost_matrix[from_node][to_node]

    transit_callback_index = routing.RegisterTransitCallback(cost_callback)
    routing.SetArcCostEvaluatorOfAllVehicles(transit_callback_index)

    # 4. Agregar restricciones de Ventanas de Tiempo (Opcional)
    if request.timeWindows:
        def time_callback(from_index, to_index):
            from_node = manager.IndexToNode(from_index)
            to_node = manager.IndexToNode(to_index)
            return time_matrix[from_node][to_node]

        time_callback_index = routing.RegisterTransitCallback(time_callback)
        
        # Agregamos una "Dimensión" de tiempo. 
        # 86400 segundos = 24 horas (máximo tiempo permitido por ruta)
        routing.AddDimension(
            time_callback_index,
            86400,  # Tiempo máximo de espera permitido
            86400,  # Tiempo máximo total por vehículo
            False,  # No forzar a que empiece en el segundo 0
            "Time"
        )
        time_dimension = routing.GetDimensionOrDie("Time")

        # Aplicar los rangos (start, end) a cada punto específico
        for i, tw in enumerate(request.timeWindows):
            if tw: 
                index = manager.NodeToIndex(i)
                time_dimension.CumulVar(index).SetRange(tw.startSecond, tw.endSecond)

    # 5. Configurar parámetros de búsqueda (Heurística)
    search_parameters = pywrapcp.DefaultRoutingSearchParameters()
    search_parameters.first_solution_strategy = (
        routing_enums_pb2.FirstSolutionStrategy.PATH_CHEAPEST_ARC)

    # 6. ¡Resolver el problema matemático!
    solution = routing.SolveWithParameters(search_parameters)

    # Si no hay solución matemática posible (ej. ventanas de tiempo incompatibles)
    if not solution:
        return {"status": "INFEASIBLE"}

    # 7. Extraer los resultados de la solución
    index = routing.Start(0)
    route_indexes = []
    total_distance = 0.0
    total_time = 0.0

    while not routing.IsEnd(index):
        node_index = manager.IndexToNode(index)
        route_indexes.append(node_index)
        
        previous_index = index
        index = solution.Value(routing.NextVar(index))
        
        # Sumar distancias y tiempos reales del trayecto
        next_node = manager.IndexToNode(index)
        total_distance += distance_matrix[node_index][next_node]
        total_time += time_matrix[node_index][next_node]

    return {
        "status": "SUCCESS",
        "route_indexes": route_indexes,
        "total_distance": float(total_distance),
        "total_time": float(total_time)
    }