# ADR-007: Arquitectura hexagonal (ports & adapters) para api-gestion

| Campo      | Valor                                    |
| ---------- | ---------------------------------------- |
| **Estado** | **Implementado (fases F0–F5)**           |
| **Fecha**  | 2026-10-02 (decisión) · 2026-10-04 (implementación) |
| **Contexto** | Migración completa del backend a arquitectura hexagonal con cumplimiento SOLID verificable |

## Contexto

El backend era una arquitectura por capas clásica (`controller → service → repository`) con
desviaciones puntuales: 6 controllers que inyectaban repositorios directo, 3 services-gigante
(`ProyectoServiceImpl` 1559 L, `ReporteServiceImpl` 1089 L, `ProjectClosureServiceImpl` 926 L)
que mezclaban responsabilidades, interfaces de servicio con contratos demasiado anchos (ISP) y
acoplamiento a tipos de Spring Web (`MultipartFile`, `Authentication`) fuera de la capa web.

El módulo `service/notification/` ya operaba con el patrón puerto/adaptador
(`NotificationSenderPort`, `*RepositoryPort`, adaptadores `Spring*`) y sirve de referencia.

## Decisión

Adoptar **arquitectura hexagonal (Cockburn) organizada por capas** con el siguiente mapa de
paquetes y reglas de dependencia, verificadas con **ArchUnit** (`ArchitectureTest`, R1–R7):

```
com.proyecta.api_gestion/
├── domain/            # núcleo: model/ (entidades ricas), exception/, value/ (FileUpload, UserContext, PageQuery, PageResult)
├── application/
│   ├── port/in/       # casos de uso (*UseCase)
│   ├── port/out/      # puertos de salida (*RepositoryPort, StoragePort, ReportPort, MailPort, SignerPort...)
│   └── service/       # implementaciones de port/in; @Transactional vive aquí
├── adapter/
│   ├── in/web/        # controllers, dto/ (contrato HTTP), GlobalExceptionHandler, filtros
│   ├── in/batch/      # schedulers @Scheduled, seeders, CommandLineRunner
│   └── out/           # persistence, mail, storage, report, audit, security
├── config/            # composition root (SecurityConfig, CORS, async, Flyway, OpenAPI, Jackson)
└── common/            # utilidades puras (sanitizers) usables por todas las capas
```

### Decisiones derivadas

1. **JPA dentro del dominio (pragmático)**: las 72 entidades conservan `@Entity` y viven en
   `domain/model/`. Excepción documentada a la pureza del núcleo, aceptada para una app
   CRUD+rica de dominio (práctica estándar hexagonal-Spring). Los puertos ocultan el JPA:
   `application` no importa `org.springframework.data` ni `jakarta.persistence`.
2. **Puertos de repositorio propios**: cada uno de los 51 repositorios Spring Data expone un
   puerto `*RepositoryPort` en `application/port/out/persistence` (declaración de firmas
   espejo); la interfaz JPA implementa el puerto
   (`interface ProyectoJpaRepository extends JpaRepository<..>, ProyectoRepositoryPort {}`),
   de modo que el adaptador es la propia interfaz Spring Data sin clases wrapper. Las
   consultas `@Query` permanecen en el lado JPA.
3. **Paginación propia**: los puertos usan `PageQuery`/`PageResult` (valores de dominio);
   `Pageable` de Spring Data solo existen en la frontera y en `adapter/out/persistence`.
   La frontera web recibe `page`/`size`/`sort` como `@RequestParam` y traduce con
   `PageSupport.fromParams` (Fase 4); la respuesta conserva la forma `Page<T>` de Spring
   (contrato JSON intacto, `PageSupport.toPage`).
4. **Sin tipos de Spring Web en la aplicación**: `MultipartFile` → `FileUpload` (dominio);
   `Authentication` → `UserContext` (dominio). La conversión ocurre en `adapter/in/web`.
5. **`@PreAuthorize` permanece en los controllers** (adaptador web): la autorización es
   control de entrada HTTP; los beans `ProyectoSecurity`/`LocalUserAuthorizationService`
   quedan en `application` y reciben `UserContext`, no `Authentication`.
6. **Los 125 DTOs pertenecen a la frontera**: los records que son contrato HTTP puro
   (request/response ensamblados solo por controllers) viven en `adapter/in/web/dto/`; los
   records que cruza la frontera de un puerto (parámetro o retorno de un `*UseCase`) viven en
   `application/port/in/dto/` como parte del contrato del caso de uso (el adaptador web los
   reutiliza o envuelve en `ApiResponse`). `domain` nunca importa DTOs.
7. **Interfaces de controller eliminadas** (12): los adaptadores web no se abstraen; los
   `@Operation`/`@Tag` de OpenAPI viven en la clase del controller (fuente única).
8. **`config/` es la composition root**: único paquete con permiso para importar todo.

## Consecuencias

- Reglas R1–R7 en `src/test/java/com/proyecta/api_gestion/architecture/ArchitectureTest.java`
  fallan el build si una dependencia viola la arquitectura.
- Los 51 repositorios ganan una interfaz espejo (mecánica de firmas) y los métodos paginados
  traducen `PageQuery ↔ Pageable` en el adaptador.
- La capa de aplicación queda sin dependencias de Spring Web/Mail/Data/Security → los tests
  unitarios mockean puertos (sin tipos de framework).
- El contrato HTTP (endpoints + JSON) y el esquema de BD (Flyway V1/V2) **no cambian**.

## Implementación (F0–F5, 2026-10-04)

Gate por fase (compile + suite completa + arranque `Started ApiGestionApplication`), sin
cambios en el contrato HTTP ni en Flyway:

| Fase | Entregable |
| ---- | ---------- |
| F0 | Estructura de paquetes `domain/application/adapter/config` |
| F1 | R1/R2 activas + pureza de dominio quirúrgica |
| F2 | 51 puertos de salida + `PageBridge` (JPA espejo, `default findAll(PageQuery)`) |
| F3 | Sin `domain→dto`, read-models en `application/readmodel`, R5–R7 |
| F4 | `Pageable` eliminado de la frontera (`PageSupport.fromParams`), R3b dividida en R3b-i/R3b-ii |
| F5 | Soportes SOLID: `FuragSupport`, `ProjectStructureSupport`, `RiesgoInicialSupport`, `ExcelSheetSupport` (`ProyectoServiceImpl` 1713 → 1099 L) |

**Reglas finales: 11** (`ArchitectureTest`): R1, R1b, R2, R3a, R3b-i, R3b-ii, R4a, R4b, R5,
R6, R7.

**Desviaciones respecto del texto original (pendientes)**: (a) los controllers siguen en
`controller/` (la exención `..controller..` está codificada en R3b-i/R5/R6) en lugar de
`adapter/in/web`; (b) no se creó `application/port/in` — los casos de uso permanecen en
`service/interfaces` con implementación en `service/impl`; (c) los DTOs de puerto siguen en
`dto/` (R6 los prohíbe en dominio/application/adapter.out, pero no los reubica);
(d) la decisión 7 (eliminar las 12 interfaces de controller) no se ejecutó.
