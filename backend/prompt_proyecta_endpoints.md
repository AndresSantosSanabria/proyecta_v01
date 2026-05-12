# PROMPT — Generación de Endpoints REST para PROYECTA
## Spring Boot 4 · Java 25 · Arquitectura Hexagonal (Puertos y Adaptadores) · SOLID · POO

---

Eres un arquitecto de software senior especializado en **Java 25** y **Spring Boot 4**.
Tu tarea es implementar los endpoints REST del sistema **PROYECTA** de la Gobernación de Cundinamarca,
siguiendo estrictamente los principios **SOLID**, **arquitectura hexagonal (Puertos y Adaptadores)**
y buenas prácticas de **Programación Orientada a Objetos (POO)**.

---

## REGLA CRÍTICA — Detección de duplicados

Antes de generar cualquier clase o endpoint, **escanea todo el código fuente existente** en el proyecto.

- Si ya existe un `@RestController`, `@Service`, Puerto o Adaptador que **cubra la misma funcionalidad**
  (mismo recurso + mismo verbo HTTP + misma intención de negocio), **omítelo completamente**.
- Si existe pero está **incompleto, mal estructurado o no sigue la arquitectura indicada**,
  **refactorízalo** en lugar de crear uno nuevo, dejando un comentario `// REFACTORED: <razón>`.
- Solo genera código **neto nuevo** cuando la funcionalidad no exista en ninguna forma.

---

## ARQUITECTURA OBLIGATORIA — Puertos y Adaptadores (Hexagonal)

Organiza **cada módulo** con la siguiente estructura de paquetes:

```
com.gobernacion.proyecta
└── <modulo>/                        (proyectos, fases, entregables, documentos, riesgos, reportes, dashboard, usuarios)
    ├── domain/
    │   ├── model/                   (Entidades de dominio puras — sin anotaciones de framework)
    │   ├── port/
    │   │   ├── in/                  (Puertos de ENTRADA — interfaces de caso de uso)
    │   │   └── out/                 (Puertos de SALIDA — interfaces de repositorio / servicios externos)
    │   └── service/                 (Implementaciones de los puertos de entrada — lógica de negocio pura)
    ├── application/
    │   └── dto/                     (DTOs de request y response — records de Java 25)
    └── infrastructure/
        ├── adapter/
        │   ├── in/
        │   │   └── web/             (Controllers REST — @RestController — adaptan HTTP → dominio)
        │   └── out/
        │       └── persistence/     (Repositorios JPA — adaptan dominio → BD)
        └── mapper/                  (MapStruct o mappers manuales dominio ↔ DTO ↔ Entity)
```

### Reglas de dependencia (Dependency Rule)
- `domain/` **no importa nada** de Spring, JPA ni capas externas.
- `application/` solo depende de `domain/`.
- `infrastructure/` depende de `domain/` y `application/` pero **nunca al revés**.
- Los `@RestController` solo conocen los **puertos de entrada** (interfaces), nunca las implementaciones.

---

## PRINCIPIOS SOLID — Aplicación concreta

### S — Single Responsibility
- Cada `UseCase` interface define **una sola operación de negocio**.
- Cada `@RestController` gestiona **un solo recurso REST** (no mezclar `/proyectos` con `/riesgos`).
- Los mappers son clases independientes; los servicios no mapean DTOs.

### O — Open/Closed
- Los servicios de dominio implementan interfaces de puerto; agregar comportamiento nuevo
  significa crear una nueva implementación o decorador, no modificar el servicio existente.
- Usa `sealed interfaces` de Java 25 para modelar resultados de operaciones
  (`Result<T>`, `CreationResult`, `NotFoundResult`, etc.).

### L — Liskov Substitution
- Toda implementación de un puerto debe ser sustituible sin alterar el comportamiento esperado.
- No lances excepciones no declaradas en el contrato del puerto.

### I — Interface Segregation
- Define puertos granulares: `CrearProyectoUseCase`, `ListarProyectosUseCase`,
  `CerrarProyectoUseCase` — no un único `ProyectoUseCase` con 10 métodos.
- Los controllers inyectan solo el puerto que necesitan.

### D — Dependency Inversion
- Los controllers y servicios dependen de **interfaces** (puertos), nunca de implementaciones concretas.
- Usa `@Component` en adaptadores y `@Service` en servicios de dominio con la anotación correcta.
- Inyección **exclusivamente por constructor** (no `@Autowired` en campo).

---

## BUENAS PRÁCTICAS POO

- Modela las entidades de dominio como clases ricas con **comportamiento propio**
  (no anemic domain model). Ejemplo: `Proyecto.calcularAvanceTotal()`, `Hito.estaVencido()`.
- Usa **records de Java 25** para DTOs inmutables.
- Usa **sealed classes/interfaces** para representar estados y resultados.
- Aplica **value objects** para conceptos del dominio:
  `CodigoProyecto`, `PonderacionPorcentual`, `CorreoElectronico`, `RangoFecha`.
- Prefiere **composición sobre herencia** para reutilizar comportamiento.
- Nombres en **español** para el dominio (reflejan el lenguaje ubicuo del negocio).
- No expongas nunca entidades JPA fuera de la capa de infraestructura.

---

## ENDPOINTS A IMPLEMENTAR

Genera el código completo para cada endpoint listado abajo.
Por cada endpoint, produce:
1. **Puerto de entrada** (`interface` en `domain/port/in/`)
2. **Servicio de dominio** (implementación del puerto en `domain/service/`)
3. **Puerto de salida** si aplica (`interface` en `domain/port/out/`)
4. **Adaptador de persistencia** (`domain/port/out/` impl en `infrastructure/adapter/out/persistence/`)
5. **DTO** de request y/o response (`application/dto/` — usar `record`)
6. **Controller REST** (`infrastructure/adapter/in/web/`)
7. **Mapper** si es necesario

---

### MÓDULO 1 — Proyectos TIC

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v1/proyectos` | Listar proyectos con filtros opcionales: `estado`, `peti`, `dependencia`, `avance`. Soporta paginación. |
| GET | `/api/v1/proyectos/{id}` | Detalle completo de un proyecto por ID. |
| POST | `/api/v1/proyectos` | Crear nuevo proyecto TIC (datos del wizard paso 1 de 6: nombre, código, vigencia, dependencia, director, correo director, patrocinador, objetivo general, objetivos específicos). |
| PUT | `/api/v1/proyectos/{id}` | Actualizar datos generales del proyecto. |
| DELETE | `/api/v1/proyectos/{id}` | Eliminar proyecto. |
| GET | `/api/v1/proyectos/{id}/resumen` | Resumen ejecutivo del proyecto para la pantalla de cierre. |
| PATCH | `/api/v1/proyectos/{id}/cerrar` | Cerrar proyecto. Valida: todos los hitos al 100% y revisados por gestor. |

#### MÓDULO 1-B — FURAG y PETI (sub-recurso de proyecto)

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v1/proyectos/{id}/furag` | Obtener respuestas FURAG actuales del proyecto (categorías a–g). |
| PUT | `/api/v1/proyectos/{id}/furag` | Guardar o actualizar respuestas FURAG. Cada categoría acepta `Sí/No` y estrategia PETI. |

---

### MÓDULO 2 — Fases, Hitos y Ponderaciones

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v1/proyectos/{id}/fases` | Listar fases del proyecto con ponderación y avance calculado automáticamente. |
| POST | `/api/v1/proyectos/{id}/fases` | Agregar fase (nombre, descripción opcional, peso %). Valida que la suma de pesos de todas las fases no supere 100%. |
| PUT | `/api/v1/proyectos/{id}/fases/{faseId}` | Editar fase (nombre, descripción, ponderación). |
| DELETE | `/api/v1/proyectos/{id}/fases/{faseId}` | Eliminar fase y sus hitos/entregables en cascada. |
| GET | `/api/v1/proyectos/{id}/fases/{faseId}/hitos` | Listar hitos de una fase con cumplimiento y ponderación. |
| POST | `/api/v1/proyectos/{id}/fases/{faseId}/hitos` | Agregar hito a la fase (nombre, descripción, peso %). Suma de pesos por fase debe ser 100%. |
| PUT | `/api/v1/proyectos/{id}/fases/{faseId}/hitos/{hitoId}` | Editar hito. |
| DELETE | `/api/v1/proyectos/{id}/fases/{faseId}/hitos/{hitoId}` | Eliminar hito y sus entregables. |

---

### MÓDULO 3 — Entregables

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v1/proyectos/{id}/entregables` | Listar todos los entregables del proyecto con estado (conforme, atrasado, pendiente, fecha límite). |
| POST | `/api/v1/proyectos/{id}/fases/{faseId}/hitos/{hitoId}/entregables` | Agregar entregable a un hito (nombre, peso %, fecha límite). Suma de pesos por hito = 100%. |
| PUT | `/api/v1/entregables/{entregableId}` | Editar entregable (nombre, peso, fecha límite). |
| PATCH | `/api/v1/entregables/{entregableId}/conformidad` | Marcar entregable como "A conformidad". Recibe un PDF de evidencia (multipart/form-data). Actualiza avance del hito y fase automáticamente. |
| DELETE | `/api/v1/entregables/{entregableId}` | Eliminar entregable. |
| GET | `/api/v1/entregables/proximos-vencer` | Entregables que vencen en los próximos N días (default: 8). Parámetro `?dias=N`. |

---

### MÓDULO 4 — Documentos del Proyecto

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v1/proyectos/{id}/documentos` | Listar todos los documentos del proyecto con su estado de carga (cargado / pendiente). |
| POST | `/api/v1/proyectos/{id}/documentos/viabilizacion` | Subir PDF de Documento de Viabilización (A-GT-FR-039 V5). `multipart/form-data`. |
| POST | `/api/v1/proyectos/{id}/documentos/acta-constitucion` | Subir PDF del Acta de Constitución. `multipart/form-data`. |
| POST | `/api/v1/proyectos/{id}/documentos/cronograma` | Subir PDF del Cronograma del Proyecto. `multipart/form-data`. |
| POST | `/api/v1/proyectos/{id}/documentos/plan-comunicaciones` | Subir PDF del Plan de Comunicaciones (solo si el proyecto tiene plan). `multipart/form-data`. |
| GET | `/api/v1/proyectos/{id}/documentos/{tipo}/descargar` | Descargar un documento del proyecto. `{tipo}`: `viabilizacion`, `acta-constitucion`, `cronograma`, `plan-comunicaciones`. |
| POST | `/api/v1/entregables/{entregableId}/evidencia` | Subir PDF de evidencia de un entregable. `multipart/form-data`. |

---

### MÓDULO 5 — Matriz de Riesgos

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v1/proyectos/{id}/riesgos` | Listar riesgos del proyecto con nivel, probabilidad, impacto y tratamiento. |
| POST | `/api/v1/proyectos/{id}/riesgos` | Registrar nuevo riesgo (descripción, probabilidad: ALTA/MEDIA/BAJA, impacto: CRÍTICO/MODERADO/BAJO, tratamiento). |
| PUT | `/api/v1/proyectos/{id}/riesgos/{riesgoId}` | Actualizar riesgo y su plan de tratamiento. |
| DELETE | `/api/v1/proyectos/{id}/riesgos/{riesgoId}` | Eliminar riesgo. |
| PATCH | `/api/v1/proyectos/{id}/riesgos/{riesgoId}/tratamiento` | Registrar verificación del tratamiento aplicado al riesgo. Marca el riesgo como tratado. |

---

### MÓDULO 6 — Reportes

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v1/reportes/proyecto/{id}` | Generar y descargar reporte PDF del estado de un proyecto específico. `produces: application/pdf`. |
| GET | `/api/v1/reportes/portafolio` | Generar y descargar reporte PDF del estado de todos los proyectos activos. |
| GET | `/api/v1/reportes/portafolio/excel` | Exportar tabla analítica del portafolio en Excel (`.xlsx`). `produces: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`. |

Los reportes aceptan filtros opcionales por query param: `?dependencia=`, `?estado=`, `?peti=`, `?estrategia=`.

---

### MÓDULO 7 — Dashboard y Métricas

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v1/dashboard/kpis` | KPIs globales: total proyectos, total atrasados, avance promedio, cantidad próximos a vencer. Calculado en tiempo real. |
| GET | `/api/v1/dashboard/avance-por-proyecto` | Lista de proyectos activos con su avance porcentual calculado. |
| GET | `/api/v1/dashboard/proyectos-por-dependencia` | Agrupación de proyectos en buen estado por dependencia responsable. |
| GET | `/api/v1/metricas/cronograma/{id}` | Vista cronograma visual anual del proyecto: fases, hitos y fechas en formato estructurado para Gantt. |
| GET | `/api/v1/metricas/portafolio` | Análisis integral del portafolio: distribución por estado, atrasos totales, cumplimiento FURAG y clasificación PETI. |

---

### MÓDULO 8 — Usuarios y Equipo

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/v1/usuarios/me` | Perfil del usuario autenticado: nombre, rol (`ADMINISTRADOR`, `GESTOR_TIC`, `DIRECTOR`), dependencia. |
| GET | `/api/v1/proyectos/{id}/equipo` | Listar integrantes del equipo de trabajo del proyecto (nombre, rol en el equipo). |
| PUT | `/api/v1/proyectos/{id}/equipo` | Actualizar integrantes del equipo (formato: un integrante por línea: `Nombre — Rol`). |
| GET | `/api/v1/proyectos/{id}/director` | Datos del director del proyecto: nombre completo, correo, cargo en la entidad. |

---

## MODELO DE DOMINIO ORIENTATIVO

Usa estas entidades como base (enriquécelas con comportamiento):

```java
// Entidad raíz de agregado
public class Proyecto {
    private CodigoProyecto codigo;          // value object
    private String nombre;
    private Vigencia vigencia;              // value object con fechaInicio, fechaFin
    private EstadoProyecto estado;          // enum: ACTIVO, EN_RETRASO, CERRADO
    private List<Fase> fases;
    private Equipo equipo;
    private DatosPatrocinador patrocinador;
    private RespuestasFurag furag;

    public PorcentajeAvance calcularAvanceTotal() { ... }
    public boolean aptoParaCierre() { ... }
    public List<Entregable> obtenerEntregablesProximosAVencer(int dias) { ... }
}

public class Fase {
    private PonderacionPorcentual peso;     // value object: valida 0-100
    private List<Hito> hitos;

    public PorcentajeAvance calcularAvance() { ... }
    public boolean sumaPonderacionHitosValida() { ... }
}

public class Hito {
    private PonderacionPorcentual peso;
    private List<Entregable> entregables;
    private boolean revisadoPorGestor;

    public boolean estaCompleto() { ... }
    public PorcentajeAvance calcularCumplimiento() { ... }
}

public class Entregable {
    private PonderacionPorcentual peso;
    private LocalDate fechaLimite;
    private EstadoEntregable estado;        // enum: PENDIENTE, A_CONFORMIDAD, ATRASADO

    public boolean estaAtrasado() { ... }
    public boolean venceEn(int dias) { ... }
}

public class Riesgo {
    private NivelRiesgo nivel;              // calculado: CRÍTICO, MODERADO, BAJO
    private Probabilidad probabilidad;      // enum: ALTA, MEDIA, BAJA
    private Impacto impacto;               // enum: CRÍTICO, MODERADO, BAJO
    private boolean tratado;

    public NivelRiesgo calcularNivel() { ... }
}
```

---

## CONVENCIONES TÉCNICAS

### Respuestas HTTP estándar
- `200 OK` — GET exitoso, PUT/PATCH exitoso.
- `201 Created` — POST exitoso. Incluye `Location` header con la URL del recurso creado.
- `204 No Content` — DELETE exitoso.
- `400 Bad Request` — Validación de dominio fallida (ponderaciones, sumas, etc.).
- `404 Not Found` — Recurso no encontrado. Respuesta: `{ "error": "PROYECTO_NO_ENCONTRADO", "id": "..." }`.
- `409 Conflict` — Intento de cerrar proyecto sin cumplir condiciones.
- `422 Unprocessable Entity` — Regla de negocio violada (suma de pesos ≠ 100%, etc.).

### Estructura de respuesta de error
```java
public record ErrorResponse(
    String codigo,
    String mensaje,
    Instant timestamp,
    Map<String, String> detalles   // para errores de validación campo a campo
) {}
```

### Validaciones obligatorias
- Usa `jakarta.validation` (`@NotBlank`, `@NotNull`, `@Min`, `@Max`, `@Email`) en los DTOs.
- Valida reglas de negocio complejas (sumas de ponderación, estado del proyecto para cierre)
  **dentro del dominio**, no en el controller.
- Captura excepciones de dominio en un `@RestControllerAdvice` global.

### Paginación
- Todos los endpoints de listado usan `Pageable` de Spring Data.
- Respuesta envuelta en `Page<T>` o en un record personalizado `PaginaResponse<T>`.

### Subida de archivos
- Usa `MultipartFile` para PDFs. Valida: tipo MIME `application/pdf`, tamaño máximo 10 MB.
- Almacena en el servicio de archivos inyectado via puerto de salida `AlmacenamientoArchivoPort`.

### Documentación
- Anota todos los endpoints con `@Operation`, `@ApiResponse` y `@Tag` de **SpringDoc OpenAPI 3**.

---

## EJEMPLO DE ESTRUCTURA COMPLETA (referencia para el patrón)

```java
// ── domain/port/in/CrearProyectoUseCase.java ──
public interface CrearProyectoUseCase {
    ProyectoId crear(CrearProyectoCommand command);
}

// ── domain/port/out/ProyectoRepositoryPort.java ──
public interface ProyectoRepositoryPort {
    void guardar(Proyecto proyecto);
    Optional<Proyecto> buscarPorId(ProyectoId id);
    List<Proyecto> listarConFiltros(FiltroProyecto filtro, Pageable pageable);
}

// ── domain/service/CrearProyectoService.java ──
@Service
@RequiredArgsConstructor
public class CrearProyectoService implements CrearProyectoUseCase {
    private final ProyectoRepositoryPort repositorio;

    @Override
    public ProyectoId crear(CrearProyectoCommand command) {
        var proyecto = Proyecto.nuevo(command.nombre(), command.codigo(), ...);
        repositorio.guardar(proyecto);
        return proyecto.getId();
    }
}

// ── application/dto/CrearProyectoRequest.java ──
public record CrearProyectoRequest(
    @NotBlank String nombre,
    @NotBlank String codigo,
    @NotBlank String dependencia,
    @NotBlank String nombreDirector,
    @Email   String correoDirector,
    @NotNull LocalDate fechaInicio
) {}

// ── infrastructure/adapter/in/web/ProyectoController.java ──
@RestController
@RequestMapping("/api/v1/proyectos")
@RequiredArgsConstructor
@Tag(name = "Proyectos TIC")
public class ProyectoController {
    private final CrearProyectoUseCase crearProyecto;
    private final ListarProyectosUseCase listarProyectos;

    @PostMapping
    @Operation(summary = "Crear nuevo proyecto TIC")
    public ResponseEntity<Void> crear(@Valid @RequestBody CrearProyectoRequest request) {
        var id = crearProyecto.crear(ProyectoMapper.toCommand(request));
        var location = URI.create("/api/v1/proyectos/" + id.valor());
        return ResponseEntity.created(location).build();
    }
}
```

---

## INSTRUCCIÓN FINAL

Genera el código módulo por módulo en el orden listado.
Antes de cada módulo, muestra un resumen de lo que encontraste en el código existente
y qué vas a crear vs. omitir vs. refactorizar.
Sé explícito cuando apliques cada principio SOLID en un comentario breve al lado del código.
