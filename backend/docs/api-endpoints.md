# Documentación de Endpoints API — api-gestion (Proyecta)

| Campo              | Valor                                          |
| ------------------ | ---------------------------------------------- |
| **Proyecto**       | api-gestion (Proyecta)                         |
| **Base URL**       | `http://localhost:8082`                         |
| **Autenticación**  | JWT Bearer (OAuth2 Resource Server + Keycloak) |
| **Versión API**    | v1                                             |
| **Total Endpoints**| **172**                                        |

> **Orden:** Módulos ordenados del más reciente al más antiguo según la fecha de creación del controlador (primer commit en git). A igual fecha se conserva el orden ya documentado y los módulos nuevos se insertan alfabéticamente por nombre de módulo. Dentro de cada módulo, los endpoints siguen el orden del inventario canónico generado desde el código fuente.

> **Fuente:** inventario de **172 endpoints** extraído de `src/main/java/com/proyecta/api_gestion/controller/` (29 controladores activos).

> **Versiones del documento:** este archivo Markdown es la fuente; también se publican `Proyecta-Documentacion-API-REST.docx` y `.pdf` en esta misma carpeta (portada, índice automático y contenido idéntico, listos para imprimir o compartir).

---

## Índice

| # | Módulo | Controllers | Fecha creación | Endpoints |
| --- | ------ | ----------- | -------------- | --------- |
| 1 | [Documentos Internos](#1-módulo-documentos-internos) | `DocumentoInternoController` | 2026-09-24 | 4 |
| 2 | [Informe de Avance](#2-módulo-informe-de-avance) | `AdvanceReportController` | 2026-09-21 | 9 |
| 3 | [Seguimiento Excel](#3-módulo-seguimiento-excel) | `SeguimientoExcelController` | 2026-09-16 | 1 |
| 4 | [Tratamientos de Riesgo](#4-módulo-tratamientos-de-riesgo) | `RiesgoTratamientoController` | 2026-09-16 | 3 |
| 5 | [Auditoría](#5-módulo-auditoría) | `AuditController` | 2026-09-09 | 5 |
| 6 | [Evidencias del Proyecto](#6-módulo-evidencias-del-proyecto) | `ProjectEvidenceController` | 2026-09-03 | 1 |
| 7 | [Evidencia Pública](#7-módulo-evidencia-pública) | `PublicEvidenceController` | 2026-07-30 | 4 |
| 8 | [Patrocinadores](#8-módulo-patrocinadores) | `PatrocinadorController` | 2026-07-30 | 1 |
| 9 | [Permisos de Usuario](#9-módulo-permisos-de-usuario) | `PermisoUsuarioController` | 2026-07-23 | 2 |
| 10 | [Listas Paramétricas](#10-módulo-listas-paramétricas) | `ListaParametricaController` | 2026-07-23 | 3 |
| 11 | [Preguntas de Cierre](#11-módulo-preguntas-de-cierre) | `ClosureQuestionController` | 2026-07-09 | 11 |
| 12 | [Plantillas de Cierre](#12-módulo-plantillas-de-cierre) | `ClosureTemplateController` | 2026-07-09 | 4 |
| 13 | [Administración de Notificaciones](#13-módulo-administración-de-notificaciones) | `NotificationAdminController` | 2026-06-18 | 11 |
| 14 | [Notificaciones In-App](#14-módulo-notificaciones-in-app) | `InAppNotificationController` | 2026-06-18 | 4 |
| 15 | [Beneficio e Impacto](#15-módulo-beneficio-e-impacto) | `ProyectoBeneficioImpactoController` | 2026-06-18 | 3 |
| 16 | [Catálogos de Configuración](#16-módulo-catálogos-de-configuración) | `ConfiguracionCatalogoController` | 2026-06-11 | 1 |
| 17 | [Analytics](#17-módulo-analytics) | `AnalyticsController` | 2026-05-28 | 2 |
| 18 | [Administración de Seguridad](#18-módulo-administración-de-seguridad) | `AdminConfiguracionController` | 2026-05-26 | 13 |
| 19 | [Autorización](#19-módulo-autorización) | `AuthorizationController` | 2026-05-26 | 3 |
| 20 | [Usuarios](#20-módulo-usuarios) | `UsuarioController` | 2026-05-11 | 2 |
| 21 | [Documentos del Proyecto](#21-módulo-documentos-del-proyecto) | `DocumentoController` | 2026-05-06 | 9 |
| 22 | [Reportes](#22-módulo-reportes) | `ReporteController` | 2026-05-05 | 14 |
| 23 | [Cronograma](#23-módulo-cronograma) | `CronogramaController` | 2026-05-05 | 3 |
| 24 | [Cierre de Proyecto](#24-módulo-cierre-de-proyecto) | `ProjectClosureController` | 2026-05-04 | 8 |
| 25 | [Riesgos](#25-módulo-riesgos) | `RiesgoController` | 2026-04-30 | 9 |
| 26 | [Jerarquía del Proyecto](#26-módulo-jerarquía-del-proyecto) | `ProjectHierarchyController` | 2026-04-29 | 12 |
| 27 | [Avance y Evidencia](#27-módulo-avance-y-evidencia) | `AvanceProyectoController` | 2026-04-24 | 12 |
| 28 | [Dashboard](#28-módulo-dashboard) | `DashboardController` | 2026-04-23 | 2 |
| 29 | [Proyectos](#29-módulo-proyectos) | `ProyectoController` | 2026-04-23 | 16 |

---

## Convenciones

### Formato de Request/Response

- Content-Type: `application/json` (salvo endpoints `multipart/form-data` y descargas de archivos)
- Codificación: UTF-8
- Fechas: ISO 8601 (`2026-07-29T14:30:00Z`)
- UUIDs: RFC 4122 (`550e8400-e29b-41d4-a716-446655440000`)
- Límites de subida (`multipart/form-data`): archivo máximo 50 MB (`MAX_FILE_SIZE`) y petición máxima 200 MB (`MAX_REQUEST_SIZE`); al superarlos responde `413 Payload Too Large`
- Respuestas estándar envueltas en `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Descripción del resultado",
  "data": { ... }
}
```

### Autenticación

- JWT Bearer emitido por **Keycloak** y validado como OAuth2 Resource Server (`KEYCLOAK_ISSUER_URI`, `KEYCLOAK_JWKS_URI`):

```
Authorization: Bearer <jwt_token>
```

- **Endpoint de sesión:** `AuthorizationController` — `GET /api/v1/authz/me` devuelve la autorización (permisos y roles) del usuario autenticado; `POST /api/v1/authz/logout` cierra la sesión en Keycloak; `GET /api/v1/authz/role-aliases` lista los alias de roles del cliente.
- Los roles se resuelven desde los claims `realm_access.roles` y `resource_access.{clientId}.roles` (client ids de `GOB_RESOURCE_CLIENT_IDS`, por defecto `proyecta-web`).
- `hasBaseAccess` exige un usuario local en Proyecta con rol funcional (o administrador); sin él responde `403`.
- **Rutas `/api/v1/public/**` son anónimas:** no requieren token; se protegen con firma HMAC del enlace y rate limit por IP (ver *Formato de error*).

### Paginación

| Parámetro   | Tipo    | Default   | Descripción                                            |
| ----------- | ------- | --------- | ------------------------------------------------------ |
| `page`      | integer | 0         | Número de página (0-indexed)                           |
| `size`      | integer | 10        | Tamaño de página (`@PageableDefault`)                  |
| `sort`      | string  | `id,DESC` | Campo y dirección de orden (algunos endpoints usan `createdAt`) |

> Algunos endpoints no usan `Pageable` sino parámetros explícitos `page`/`size` (p. ej. `GET /api/v1/documentos-internos`, con `size` entre 1 y 200).

### Control de Acceso

| Prefijo de URL              | Control de acceso (según `@PreAuthorize`)                                | Notas                                    |
| --------------------------- | ------------------------------------------------------------------------ | ---------------------------------------- |
| `/api/v1/proyectos/**`      | `canAccess(...)`, `canAccessOperational('<PERMISO>', #proyectoId, ...)`  | Permisos por proyecto (`PROYECTO:VER`, `PROYECTO:EDITAR`, `DOCUMENTO:CARGAR`, `CRONOGRAMA:CARGAR`, `EVIDENCIA:CARGAR`, `CIERRE:*`, `REPORTE:DESCARGAR_ACTUAL`) |
| `/api/v1/admin/...`         | `canAccessGlobal('SISTEMA:CONFIGURAR', ...)`                             | Administración (usuarios, roles, notificaciones, auditoría) |
| `/api/v1/configuracion/...` | `hasBaseAccess` (lectura), `canAccessGlobal('SISTEMA:CONFIGURAR')` (escritura de listas) | Catálogos y listas paramétricas          |
| `/api/v1/reportes`, `/api/v1/analytics` | `canAccessGlobal('PROYECTO:VER', ...)` o `canAccessOperational(...)` | Descargas de portafolio propio con `canAccessOwnProjects` |
| `/api/v1/public/...`        | **Sin autenticación** (`@PublicEndpoint`)                                | Firma HMAC + rate limit por IP           |

### Códigos de éxito no estándar

| Código | Endpoint                                                              | Condición                          |
| ------ | --------------------------------------------------------------------- | ---------------------------------- |
| `201`  | `POST /api/v1/proyectos/{proyectoId}/cronograma`                      | Carga/reemplazo del PDF            |
| `201`  | `POST /api/v1/proyectos/{proyectoId}/documentos/{tipoDocumento}`      | Carga/reemplazo del documento      |
| `201`  | `POST /api/v1/documentos-internos`                                    | Carga de documento interno         |
| `201`  | `POST /api/v1/proyectos/registro-inicial`                             | Registro inicial de proyecto       |
| `201`  | `POST /api/v1/proyectos/{proyectoId}/riesgos`                         | Creación de riesgo                 |
| `201`  | `POST /api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}/soluciones`   | Carga de soluciones (archivos)     |
| `204`  | `GET /api/v1/dashboard/avance-por-proyecto`                           | Sin proyectos que mostrar          |
| `204`  | `DELETE /api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}`            | Eliminación exitosa del riesgo     |

### Formato de error

- **400 / 403 / 404 / 413 / 422 / 500** → **Problem Details, RFC 9457** (`GlobalExceptionHandler` → `ProblemDetailDTO`):

```json
{
  "type": "/errors/not-found",
  "title": "Recurso no encontrado",
  "status": 404,
  "detail": "Proyecto no encontrado: PROY-CUN-2026-999"
}
```

- **401 Unauthorized** → formato propio (`JwtAuthenticationEntryPoint`), distinto de RFC 9457:

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Token de acceso ausente, inválido o vencido. Renueve el token o inicie sesión nuevamente.",
  "path": "/api/v1/proyectos/PROY-CUN-2026-008",
  "timestamp": "2026-04-29T12:00:00",
  "action": "REFRESH_OR_LOGIN"
}
```

- **429 Too Many Requests** → solo en los endpoints públicos `/api/v1/public/**` (`PublicEvidenceSecurityFilter`, rate limit por IP, por defecto 120 req/min vía `PUBLIC_EVIDENCE_RATE_LIMIT`) con cabecera **`Retry-After: 60`**.
- **404 genérico en rutas públicas** → si la firma HMAC del enlace no es válida, la respuesta es `{"message":"Recurso no encontrado"}` (sin Problem Details).

### Swagger UI / OpenAPI

- `springdoc-openapi` está **apagado por defecto por seguridad (CWE-200)**. Para consultarlo en local hay que arrancar con:

```
SPRINGDOC_API_DOCS_ENABLED=true
SPRINGDOC_SWAGGER_UI_ENABLED=true
```

- Rutas: `/swagger-ui.html` (Swagger UI) y `/v3/api-docs` (documento OpenAPI).
- `ErrorResponseOperationCustomizer` añade a cada operación los códigos de error reales (ver siguiente sección).

### Códigos de Respuesta HTTP Globales (Swagger / OpenAPI)

El proyecto utiliza `springdoc-openapi` junto con un `ErrorResponseOperationCustomizer` para enriquecer y estandarizar automáticamente la documentación de los 172 endpoints.

Todos los endpoints enumerados en este documento responden con los siguientes códigos HTTP (documentados globalmente en Swagger UI):

- **200 / 201 / 204:** Operación exitosa (el payload de éxito varía según el endpoint; ver *Códigos de éxito no estándar*).
- **400 Bad Request:** Aplicable automáticamente si el endpoint acepta entradas (`body`, `query` o `path`). Indica errores de validación.
- **401 Unauthorized:** Aplicable a todos los endpoints privados. Indica token JWT (Keycloak) ausente o inválido.
- **403 Forbidden:** Aplicable a endpoints privados. El usuario carece de permisos suficientes (permiso por proyecto, `hasBaseAccess` o rol global).
- **404 Not Found:** Aplicado dinámicamente a endpoints con variables en la ruta (`/{id}`); en rutas públicos también por firma HMAC inválida.
- **413 Payload Too Large:** Documentado automáticamente para endpoints de tipo `multipart/form-data`.
- **422 Unprocessable Entity:** Aplicable a endpoints de escritura (`POST`, `PUT`, `PATCH`, `DELETE`). Indica que se incumplió una regla de negocio.
- **429 Too Many Requests:** Documentado en endpoints anotados con `@PublicEndpoint` para proteger contra abuso.
- **500 Internal Server Error:** Documentado globalmente para todas las operaciones (fallback de errores no capturados).

> **Nota:** Todos los errores salvo el 401 siguen el estándar **Problem Details for HTTP APIs (RFC 9457)**.

---

## 1. Módulo Documentos Internos

**Base path:** `/api/v1/documentos-internos`
**Auth:** `@localUserAuthorization.hasBaseAccess(...)` (clase) · `DOCUMENTO_INTERNO:VER` / `DOCUMENTO_INTERNO:CARGAR` (método)
**Controller:** `DocumentoInternoController` | **Creado:** 2026-09-24

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `GET` | `/api/v1/documentos-internos` | Listar documentación interna con filtros opcionales y paginación | `DOCUMENTO_INTERNO:VER` |
| 2 | `GET` | `/api/v1/documentos-internos/{id}/descargar` | Descargar un documento interno | `DOCUMENTO_INTERNO:VER` |
| 3 | `GET` | `/api/v1/documentos-internos/{id}/ver` | Ver documento interno (PDF en línea) | `DOCUMENTO_INTERNO:VER` |
| 4 | `POST` | `/api/v1/documentos-internos` | Cargar un documento interno (`multipart/form-data`; código automático DOC-ANIO-MES-DIA-HH:MM) | `DOCUMENTO_INTERNO:CARGAR` |

---

## 2. Módulo Informe de Avance

**Base path:** `/api/v1/advance-report`
**Auth:** sin `@PreAuthorize` de clase; por endpoint: `PROYECTO:VER` (operativo), `isAuthenticated()`, `SISTEMA:CONFIGURAR`, `canUploadAdvanceReport(...)`, `canReviewAdvanceReport(...)`
**Controller:** `AdvanceReportController` | **Creado:** 2026-09-21

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `GET` | `/api/v1/advance-report/download/{projectId}` | Descargar el PDF del informe de avance | `PROYECTO:VER` (operativo) |
| 2 | `GET` | `/api/v1/advance-report/pending` | Listar proyectos con informe de avance pendiente | `isAuthenticated()` |
| 3 | `PUT` | `/api/v1/advance-report/return/{projectId}` | Devolver el informe de avance al director con observaciones | `canReviewAdvanceReport(...)` |
| 4 | `GET` | `/api/v1/advance-report/settings` | Obtener la configuración del motor de reglas del informe de avance | `isAuthenticated()` |
| 5 | `PUT` | `/api/v1/advance-report/settings` | Actualizar la configuración del motor de reglas | `SISTEMA:CONFIGURAR` (global) |
| 6 | `GET` | `/api/v1/advance-report/status/{projectId}` | Obtener el estado del informe de avance de un proyecto | `PROYECTO:VER` (operativo) |
| 7 | `POST` | `/api/v1/advance-report/upload/{projectId}` | Cargar el informe de avance del proyecto | `canUploadAdvanceReport(...)` |
| 8 | `PUT` | `/api/v1/advance-report/verify/{projectId}` | Verificar (aprobar) el informe de avance del periodo actual | `canReviewAdvanceReport(...)` |
| 9 | `GET` | `/api/v1/advance-report/versions/{projectId}` | Listar el historial de versiones del informe de avance | `PROYECTO:VER` (operativo) |

---

## 3. Módulo Seguimiento Excel

**Base path:** `/api/v1/proyectos`
**Auth:** `@proyectoSecurity.canAccessOperational('PROYECTO:VER', #projectId, authentication)`
**Controller:** `SeguimientoExcelController` | **Creado:** 2026-09-16

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `GET` | `/api/v1/proyectos/{projectId}/reportes/seguimiento-excel` | Descargar el reporte de seguimiento del proyecto en Excel |

---

## 4. Módulo Tratamientos de Riesgo

**Base path:** `/api/v1/proyectos`
**Auth:** por endpoint: `canAccessOperational('PROYECTO:VER' | 'PROYECTO:EDITAR', #proyectoId, ...)`
**Controller:** `RiesgoTratamientoController` | **Creado:** 2026-09-16

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `GET` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}/tratamientos` | Listar historial de tratamientos de un riesgo | `PROYECTO:VER` |
| 2 | `POST` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}/tratamientos` | Crear un nuevo tratamiento para un riesgo | `PROYECTO:EDITAR` |
| 3 | `GET` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}/tratamientos/{tratamientoId}/adjuntos/{adjuntoId}/descargar` | Descargar un adjunto de un tratamiento específico | `PROYECTO:VER` |

---

## 5. Módulo Auditoría

**Base path:** `/api/v1/admin/auditoria`
**Auth:** `@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)`
**Controller:** `AuditController` | **Creado:** 2026-09-09

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `GET` | `/api/v1/admin/auditoria/logs` | Listar logs de auditoría con filtros y paginación |
| 2 | `GET` | `/api/v1/admin/auditoria/logs/{id}` | Obtener el detalle de un registro de auditoría |
| 3 | `PATCH` | `/api/v1/admin/auditoria/logs/{id}/eliminar` | Eliminar (lógicamente) un registro de auditoría |
| 4 | `PATCH` | `/api/v1/admin/auditoria/logs/{id}/restaurar` | Restaurar un registro de auditoría eliminado |
| 5 | `GET` | `/api/v1/admin/auditoria/stats` | Obtener estadísticas del log de auditoría |

---

## 6. Módulo Evidencias del Proyecto

**Base path:** `/api/v1/proyectos`
**Auth:** `@localUserAuthorization.hasBaseAccess(...)` (clase) · `canAccessOperational('PROYECTO:VER', #proyectoId, ...)`
**Controller:** `ProjectEvidenceController` | **Creado:** 2026-09-03

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `GET` | `/api/v1/proyectos/{proyectoId}/evidencias` | Listar las evidencias de un proyecto |

---

## 7. Módulo Evidencia Pública

**Base path:** `/api/v1/public`
**Auth:** **Sin autenticación** (`@PublicEndpoint`; firma HMAC del enlace + rate limit por IP)
**Controller:** `PublicEvidenceController` | **Creado:** 2026-07-30

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `GET` | `/api/v1/public/cierre-evidencia/{fileName}` | Descargar el PDF de cierre y transferencia de evidencia |
| 2 | `GET` | `/api/v1/public/evidencia/{token}` | Consultar una evidencia mediante token opaco |
| 3 | `GET` | `/api/v1/public/riesgos/{proyectoId}/{riesgoId}/soluciones/{solucionId}` | Descargar la solución de un riesgo en PDF |
| 4 | `GET` | `/api/v1/public/riesgos/{proyectoId}/{riesgoId}/tratamientos/{tratamientoId}/adjuntos/{adjuntoId}` | Descargar un adjunto de tratamiento de riesgo en PDF |

---

### 7.2 GET `/api/v1/public/evidencia/{token}`

Permite acceder a la evidencia de un entregable mediante un token seguro, sin autenticación.

**Response 200:** PDF (`application/pdf`, inline)
**Response 404:** Token inválido o evidencia no encontrada
**Response 429:** Rate limit por IP (cabecera `Retry-After: 60`)

---

## 8. Módulo Patrocinadores

**Base path:** `/api/v1/patrocinadores`
**Auth:** `@localUserAuthorization.hasBaseAccess(authentication)`
**Controller:** `PatrocinadorController` | **Creado:** 2026-07-30

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `GET` | `/api/v1/patrocinadores` | Listar los patrocinadores únicos registrados |

---

## 9. Módulo Permisos de Usuario

**Base path:** `/api/v1/admin/configuracion/permisos-usuario`
**Auth:** `@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")`
**Controller:** `PermisoUsuarioController` | **Creado:** 2026-07-23

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `PUT` | `/api/v1/admin/configuracion/permisos-usuario` | Actualizar la matriz de permisos de un usuario |
| 2 | `GET` | `/api/v1/admin/configuracion/permisos-usuario/{usuarioId}` | Obtener la matriz de permisos de un usuario |

---

## 10. Módulo Listas Paramétricas

**Base path:** `/api/v1/configuracion/listas`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")` (clase); escritura con `SISTEMA:CONFIGURAR`
**Controller:** `ListaParametricaController` | **Creado:** 2026-07-23

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `GET` | `/api/v1/configuracion/listas/{listaClave}` | Listar ítems de una lista paramétrica | `hasBaseAccess` |
| 2 | `GET` | `/api/v1/configuracion/listas/{listaClave}/valores` | Listar valores activos de una lista paramétrica | `hasBaseAccess` |
| 3 | `PUT` | `/api/v1/configuracion/listas/{listaClave}/valores` | Reemplazar los valores de una lista paramétrica | `SISTEMA:CONFIGURAR` |

---

## 11. Módulo Preguntas de Cierre

**Base path:** `/api/v1/admin/closure-questions`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")` (clase)
**Controller:** `ClosureQuestionController` | **Creado:** 2026-07-09

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `GET` | `/api/v1/admin/closure-questions` | Listar todas las preguntas de cierre | `SISTEMA:CONFIGURAR` |
| 2 | `POST` | `/api/v1/admin/closure-questions` | Crear una pregunta de cierre | `SISTEMA:CONFIGURAR` |
| 3 | `DELETE` | `/api/v1/admin/closure-questions/{id}` | Eliminar una pregunta de cierre | `SISTEMA:CONFIGURAR` |
| 4 | `PATCH` | `/api/v1/admin/closure-questions/{id}` | Actualizar una pregunta de cierre | `SISTEMA:CONFIGURAR` |
| 5 | `PATCH` | `/api/v1/admin/closure-questions/{id}/toggle` | Alternar el estado activo de una pregunta de cierre | `SISTEMA:CONFIGURAR` |
| 6 | `GET` | `/api/v1/admin/closure-questions/active` | Listar las preguntas de cierre activas | `SISTEMA:CONFIGURAR` |
| 7 | `GET` | `/api/v1/admin/closure-questions/answers/{projectId}` | Listar las respuestas de cierre de un proyecto | `PROYECTO:VER` (operativo) |
| 8 | `POST` | `/api/v1/admin/closure-questions/answers/{projectId}` | Guardar las respuestas del checklist de cierre | `CIERRE:SOLICITAR` (operativo) |
| 9 | `GET` | `/api/v1/admin/closure-questions/draft/{projectId}` | Obtener el borrador de plantilla de cierre del proyecto | `PROYECTO:VER` (operativo) |
| 10 | `GET` | `/api/v1/admin/closure-questions/missing/{projectId}` | Listar las preguntas de cierre faltantes de un proyecto | `PROYECTO:VER` (operativo) |
| 11 | `GET` | `/api/v1/admin/closure-questions/resolved-template/{projectId}` | Obtener la plantilla de cierre resuelta con las respuestas | `PROYECTO:VER` (operativo) |

---

### 11.8 POST `/api/v1/admin/closure-questions/answers/{projectId}`

Guarda las respuestas del cuestionario de cierre para un proyecto.

**Body:**
```json
[
  { "questionId": 1, "respuesta": "Sí, se cumplió el objetivo" },
  { "questionId": 2, "respuesta": "El proyecto generó impacto positivo" }
]
```

**Response 200:** `ApiResponse<Void>`

---

## 12. Módulo Plantillas de Cierre

**Base path:** `/api/v1/admin/closure-templates`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")` (clase)
**Controller:** `ClosureTemplateController` | **Creado:** 2026-07-09

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `POST` | `/api/v1/admin/closure-templates` | Guardar una nueva versión de la plantilla de cierre | `SISTEMA:CONFIGURAR` |
| 2 | `GET` | `/api/v1/admin/closure-templates/active` | Obtener la plantilla de cierre activa | `SISTEMA:CONFIGURAR` |
| 3 | `GET` | `/api/v1/admin/closure-templates/closure-record/{projectId}` | Obtener el registro formal de cierre de un proyecto | `PROYECTO:VER` (operativo) |
| 4 | `POST` | `/api/v1/admin/closure-templates/closure-record/{projectId}` | Guardar o actualizar el registro formal de cierre de un proyecto | `CIERRE:SOLICITAR` (operativo) |

---

## 13. Módulo Administración de Notificaciones

**Base path:** `/api/v1/admin/notificaciones`
**Auth:** `@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")`
**Controller:** `NotificationAdminController` | **Creado:** 2026-06-18

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `GET` | `/api/v1/admin/notificaciones/diagnostico` | Obtener diagnóstico de la configuración de correo |
| 2 | `GET` | `/api/v1/admin/notificaciones/estadisticas` | Obtener estadísticas del sistema de notificaciones |
| 3 | `GET` | `/api/v1/admin/notificaciones/eventos` | Listar el catálogo de eventos de notificación |
| 4 | `GET` | `/api/v1/admin/notificaciones/fallidas` | Listar notificaciones fallidas |
| 5 | `GET` | `/api/v1/admin/notificaciones/fallidas/detalle-dispatch` | Listar trazas de despacho fallidas de correo |
| 6 | `GET` | `/api/v1/admin/notificaciones/plantillas` | Listar las plantillas de notificación configuradas |
| 7 | `PUT` | `/api/v1/admin/notificaciones/plantillas` | Crear o actualizar una plantilla de notificación |
| 8 | `POST` | `/api/v1/admin/notificaciones/plantillas/preview` | Previsualizar una plantilla de notificación |
| 9 | `POST` | `/api/v1/admin/notificaciones/plantillas/test-send` | Enviar un correo de prueba de notificación |
| 10 | `GET` | `/api/v1/admin/notificaciones/preferencias` | Listar las preferencias de notificación de los usuarios |
| 11 | `PUT` | `/api/v1/admin/notificaciones/preferencias` | Crear o actualizar una preferencia de notificación |

---

### 13.9 POST `/api/v1/admin/notificaciones/plantillas/test-send`

Envía un correo de prueba al usuario autenticado.

**Body:**
```json
{
  "subjectTemplate": "Prueba: {{proyecto_nombre}}",
  "bodyTemplate": "<h1>Test</h1><p>Proyecto: {{proyecto_nombre}}</p>",
  "htmlEnabled": true,
  "variables": { "proyecto_nombre": "Proyecto de Prueba" }
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Correo de prueba procesado",
  "data": {
    "recipient": "juan.perez@domain.com",
    "status": "SENT",
    "success": true
  }
}
```

---

## 14. Módulo Notificaciones In-App

**Base path:** `/api/v1/notificaciones`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `InAppNotificationController` | **Creado:** 2026-06-18

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `GET` | `/api/v1/notificaciones` | Listar notificaciones in-app del usuario (paginado) |
| 2 | `PATCH` | `/api/v1/notificaciones/{id}/leer` | Marcar una notificación como leída |
| 3 | `PATCH` | `/api/v1/notificaciones/marcar-todas-leidas` | Marcar todas las notificaciones del usuario como leídas |
| 4 | `GET` | `/api/v1/notificaciones/no-leidas` | Contar notificaciones no leídas |

---

### 14.1 GET `/api/v1/notificaciones`

**Query params:**
| Param | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `leido` | boolean | No | Filtrar por estado de lectura |
| `eventCode` | string | No | Filtrar por código del evento de notificación |
| `page` | integer | No | Página |
| `size` | integer | No | Tamaño de página |

**Response 200:** `Page<InAppNotificationDTO>`

---

## 15. Módulo Beneficio e Impacto

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")` (clase)
**Controller:** `ProyectoBeneficioImpactoController` | **Creado:** 2026-06-18

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `GET` | `/api/v1/proyectos/{proyectoId}/beneficio-impacto` | Obtener la información de beneficio e impacto de un proyecto | `canViewBenefitImpact` |
| 2 | `PUT` | `/api/v1/proyectos/{proyectoId}/beneficio-impacto` | Guardar la información de beneficio e impacto de un proyecto | `canEditBenefitImpact` |
| 3 | `POST` | `/api/v1/proyectos/{proyectoId}/beneficio-impacto/revision` | Revisar la información de beneficio e impacto | `canReviewBenefitImpact` |

---

### 15.3 POST `/api/v1/proyectos/{proyectoId}/beneficio-impacto/revision`

**Query params:**
| Param | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `aprobado` | boolean | Sí | true para aprobar, false para observar |
| `observaciones` | string | No | Observaciones en caso de rechazo |

---

## 16. Módulo Catálogos de Configuración

**Base path:** `/api/v1/configuracion/catalogos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `ConfiguracionCatalogoController` | **Creado:** 2026-06-11

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `GET` | `/api/v1/configuracion/catalogos/peti` | Obtener el catálogo PETI |

---

## 17. Módulo Analytics

**Base path:** `/api/v1/analytics`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")` (clase)
**Controller:** `AnalyticsController` | **Creado:** 2026-05-28

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `GET` | `/api/v1/analytics/portafolio` | Obtener la analítica del portafolio de proyectos | `PROYECTO:VER` (global) |
| 2 | `GET` | `/api/v1/analytics/portafolio/pdf` | Descargar la analítica del portafolio en PDF | `PROYECTO:VER` (global) |

---

## 18. Módulo Administración de Seguridad

**Base path:** `/api/v1/admin/configuracion`
**Auth:** `@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")`
**Controller:** `AdminConfiguracionController` | **Creado:** 2026-05-26

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `GET` | `/api/v1/admin/configuracion/cargos-asignacion` | Listar cargos disponibles para asignación |
| 2 | `GET` | `/api/v1/admin/configuracion/permisos` | Listar permisos del sistema |
| 3 | `GET` | `/api/v1/admin/configuracion/roles` | Listar roles activos del sistema |
| 4 | `POST` | `/api/v1/admin/configuracion/roles` | Crear un rol nuevo |
| 5 | `DELETE` | `/api/v1/admin/configuracion/roles/{codigo}` | Desactivar un rol |
| 6 | `PUT` | `/api/v1/admin/configuracion/roles/{codigo}` | Actualizar un rol existente |
| 7 | `GET` | `/api/v1/admin/configuracion/roles/todos` | Listar todos los roles del sistema |
| 8 | `PUT` | `/api/v1/admin/configuracion/roles-permisos` | Actualizar la matriz de roles y permisos |
| 9 | `GET` | `/api/v1/admin/configuracion/storage-path/test` | Validar una ruta de almacenamiento de archivos |
| 10 | `GET` | `/api/v1/admin/configuracion/usuario-proyecto` | Listar proyectos asignados a un usuario |
| 11 | `POST` | `/api/v1/admin/configuracion/usuario-proyecto` | Asignar un usuario a un proyecto |
| 12 | `GET` | `/api/v1/admin/configuracion/usuarios` | Listar usuarios del sistema |
| 13 | `PUT` | `/api/v1/admin/configuracion/usuarios` | Actualizar los datos de un usuario |

> Los endpoints de matriz de permisos de un usuario (`PUT`/`GET /api/v1/admin/configuracion/permisos-usuario`) están documentados en el [módulo 9](#9-módulo-permisos-de-usuario) (`PermisoUsuarioController`).

---

### 18.8 PUT `/api/v1/admin/configuracion/roles-permisos`

**Body:**
```json
{
  "matrices": [
    {
      "rolCodigo": "ADMINISTRADOR",
      "permisos": ["PROYECTO:VER", "PROYECTO:EDITAR", "SISTEMA:CONFIGURAR"]
    }
  ]
}
```

**Response 200:** `ApiResponse<Void>`

---

### 18.12 GET `/api/v1/admin/configuracion/usuarios`

**Query params:**
| Param | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `search` | string | No | Búsqueda por nombre/username |
| `rol` | string | No | Filtrar por rol |
| `page` | integer | No | Página |
| `size` | integer | No | Tamaño |

**Response 200:** `Page<SeguridadUsuarioDTO>`

---

## 19. Módulo Autorización

**Base path:** `/api/v1/authz`
**Auth:** `@PreAuthorize("isAuthenticated()")` por endpoint
**Controller:** `AuthorizationController` | **Creado:** 2026-05-26

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `POST` | `/api/v1/authz/logout` | Cerrar sesión en Keycloak |
| 2 | `GET` | `/api/v1/authz/me` | Obtener la autorización del usuario autenticado |
| 3 | `GET` | `/api/v1/authz/role-aliases` | Listar los alias de roles del cliente |

---

## 20. Módulo Usuarios

**Base path:** `/api/v1/usuarios`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `UsuarioController` | **Creado:** 2026-05-11

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `GET` | `/api/v1/usuarios/me` | Obtener perfil del usuario autenticado |
| 2 | `PATCH` | `/api/v1/usuarios/me/notificaciones-globales` | Actualizar flag de notificaciones globales del usuario autenticado |

---

### 20.1 GET `/api/v1/usuarios/me`

**Response 200:**
```json
{
  "success": true,
  "message": "Perfil de usuario recuperado",
  "data": {
    "id": 1,
    "nombre": "Juan Pérez",
    "correo": "juan.perez@domain.com",
    "rolCodigo": "ADMINISTRADOR",
    "rolNombre": "Administrador del Sistema",
    "nivelAcceso": 1,
    "dependencia": "Secretaría TIC",
    "activo": true,
    "ultimoAcceso": "2026-07-29T14:30:00Z",
    "recibirNotificacionesGlobales": true
  }
}
```

---

### 20.2 PATCH `/api/v1/usuarios/me/notificaciones-globales`

**Body:**
```json
{ "recibirNotificacionesGlobales": true }
```

---

## 21. Módulo Documentos del Proyecto

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")` (clase)
**Controller:** `DocumentoController` | **Creado:** 2026-05-06

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `GET` | `/api/v1/proyectos/{proyectoId}/documentos` | Listar todos los documentos cargados al proyecto | `PROYECTO:VER` |
| 2 | `POST` | `/api/v1/proyectos/{proyectoId}/documentos/{tipoDocumento}` | Cargar o reemplazar un documento del proyecto (requiere observación al reemplazar) | `DOCUMENTO:CARGAR` |
| 3 | `GET` | `/api/v1/proyectos/{proyectoId}/documentos/{tipoDocumento}/descargar` | Descargar el documento actual del proyecto | `PROYECTO:VER` |
| 4 | `GET` | `/api/v1/proyectos/{proyectoId}/documentos/{tipoDocumento}/versiones` | Listar historial de versiones de un documento | `PROYECTO:VER` |
| 5 | `GET` | `/api/v1/proyectos/{proyectoId}/documentos/{tipoDocumento}/versiones/{numeroVersion}/archivo` | Descargar una versión específica de un documento | `PROYECTO:VER` |
| 6 | `GET` | `/api/v1/proyectos/{proyectoId}/documentos-pre-wizard` | Listar revisiones de los 3 documentos pre-wizard | `PROYECTO:VER` |
| 7 | `PATCH` | `/api/v1/proyectos/{proyectoId}/documentos-pre-wizard/{tipoDocumento}/aprobar` | Aprobar individualmente un documento pre-wizard | `PROYECTO:EDITAR` |
| 8 | `PATCH` | `/api/v1/proyectos/{proyectoId}/documentos-pre-wizard/{tipoDocumento}/devolver` | Devolver individualmente un documento pre-wizard con observaciones | `PROYECTO:EDITAR` |
| 9 | `POST` | `/api/v1/proyectos/{proyectoId}/documentos-pre-wizard/confirmar-revision` | Confirmar revisión pre-wizard y enviar notificación consolidada | `PROYECTO:EDITAR` |

---

### 21.2 POST `/api/v1/proyectos/{proyectoId}/documentos/{tipoDocumento}`

**Content-Type:** `multipart/form-data`

| Campo | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `archivo` | MultipartFile | Sí | Archivo del documento |
| `observacion` | string | No | Observación opcional |

**Response 201:** `DocumentoUploadResultDTO`

---

## 22. Módulo Reportes

**Base path:** `/api/v1/reportes`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")` (clase)
**Controller:** `ReporteController` | **Creado:** 2026-05-05

| # | Método | Endpoint | Descripción | Permiso | Formato |
| --- | ------ | -------- | ----------- | ------- | ------- |
| 1 | `GET` | `/api/v1/reportes/configuracion` | Obtener configuración de reportes | `PROYECTO:VER` (global) | JSON |
| 2 | `GET` | `/api/v1/reportes/furag/{proyectoId}` | Obtener reporte FURAG | `PROYECTO:VER` (operativo) | JSON |
| 3 | `GET` | `/api/v1/reportes/furag/{proyectoId}/descargar` | Descargar reporte PDF FURAG | `PROYECTO:VER` (operativo) | PDF |
| 4 | `GET` | `/api/v1/reportes/plan-comunicaciones/descargar` | Descargar reporte PDF de plan de comunicaciones | `PROYECTO:VER` (global) | PDF |
| 5 | `GET` | `/api/v1/reportes/portafolio/descargar` | Descargar reporte PDF de portafolio | `PROYECTO:VER` (global) | PDF |
| 6 | `GET` | `/api/v1/reportes/portafolio/excel` | Exportar portafolio a Excel | `PROYECTO:VER` (propios) | XLSX |
| 7 | `GET` | `/api/v1/reportes/proyecto/{id}/descargar` | Descargar reporte PDF de proyecto | `PROYECTO:VER` (operativo) | PDF |
| 8 | `GET` | `/api/v1/reportes/proyecto/{proyectoId}/excel` | Descargar reporte actual del proyecto en Excel | `REPORTE:DESCARGAR_ACTUAL` | XLSX |
| 9 | `GET` | `/api/v1/reportes/proyectos-con-retrasos` | Obtener proyectos con retrasos | `PROYECTO:VER` (global) | JSON |
| 10 | `GET` | `/api/v1/reportes/proyectos-con-retrasos/descargar` | Descargar reporte PDF de proyectos con retrasos | `PROYECTO:VER` (global) | PDF |
| 11 | `GET` | `/api/v1/reportes/riesgos` | Obtener verificación de tratamiento a riesgos | `PROYECTO:VER` (global) | JSON |
| 12 | `GET` | `/api/v1/reportes/riesgos/descargar` | Descargar reporte PDF de riesgos | `PROYECTO:VER` (global) | PDF |
| 13 | `GET` | `/api/v1/reportes/todos-los-proyectos` | Obtener estado de todos los proyectos | `PROYECTO:VER` (global) | JSON |
| 14 | `GET` | `/api/v1/reportes/vista-previa/{proyectoId}` | Obtener vista previa de reporte | `PROYECTO:VER` (operativo) | JSON |

---

### 22.6 GET `/api/v1/reportes/portafolio/excel`

**Query params:**
| Param | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `query` | string | No | Búsqueda por nombre |
| `dependency` | string | No | Filtrar por dependencia |
| `status` | string | No | Filtrar por estado |
| `peti` | string | No | Filtrar por PETI |

---

## 23. Módulo Cronograma

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")` (clase)
**Controller:** `CronogramaController` | **Creado:** 2026-05-05

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `GET` | `/api/v1/proyectos/{proyectoId}/cronograma` | Obtener el cronograma del proyecto | `PROYECTO:VER` |
| 2 | `POST` | `/api/v1/proyectos/{proyectoId}/cronograma` | Cargar o reemplazar el PDF del cronograma (multipart) — **201** | `CRONOGRAMA:CARGAR` |
| 3 | `GET` | `/api/v1/proyectos/{proyectoId}/cronograma/descargar` | Descargar el PDF del cronograma | `PROYECTO:VER` |

---

## 24. Módulo Cierre de Proyecto

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")` (clase)
**Controller:** `ProjectClosureController` | **Creado:** 2026-05-04

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `POST` | `/api/v1/proyectos/{id}/cierre` | Ejecutar cierre formal del proyecto | `PROYECTO:CERRAR` |
| 2 | `POST` | `/api/v1/proyectos/{id}/cierre/aprobar` | Aprobar solicitud de cierre | `CIERRE:APROBAR` |
| 3 | `GET` | `/api/v1/proyectos/{id}/cierre/descargar` | Descargar acta de cierre | `PROYECTO:VER` |
| 4 | `POST` | `/api/v1/proyectos/{id}/cierre/evidencia-transferencia` | Subir evidencia de transferencia de conocimiento (`multipart/form-data`) | `CIERRE:SOLICITAR` |
| 5 | `GET` | `/api/v1/proyectos/{id}/cierre/evidencia-transferencia/{fileName}` | Descargar evidencia de transferencia de conocimiento | `PROYECTO:VER` |
| 6 | `POST` | `/api/v1/proyectos/{id}/cierre/extraordinario` | Cierre extraordinario del proyecto | `CIERRE:EXTRAORDINARIO` |
| 7 | `POST` | `/api/v1/proyectos/{id}/cierre/rechazar` | Rechazar solicitud de cierre | `CIERRE:APROBAR` |
| 8 | `POST` | `/api/v1/proyectos/{id}/cierre/solicitar` | Solicitar cierre de proyecto | `CIERRE:SOLICITAR` |

---

### 24.7 POST `/api/v1/proyectos/{id}/cierre/rechazar`

**Body:**
```json
{ "observaciones": "Faltan entregables por revisar" }
```

---

### 24.8 POST `/api/v1/proyectos/{id}/cierre/solicitar`

**Body:**
```json
{
  "observaciones": "Proyecto cumplió todos los entregables",
  "fechaCierre": "2026-07-29"
}
```

**Response 200:** `CierreProyectoResponse`

---

## 25. Módulo Riesgos

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")` (clase)
**Controller:** `RiesgoController` | **Creado:** 2026-04-30

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `GET` | `/api/v1/proyectos/{proyectoId}/riesgos` | Listar todos los riesgos de un proyecto | `PROYECTO:VER` |
| 2 | `POST` | `/api/v1/proyectos/{proyectoId}/riesgos` | Agregar un nuevo riesgo al proyecto — **201** | `PROYECTO:EDITAR` |
| 3 | `DELETE` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}` | Eliminar un riesgo — **204** | `PROYECTO:EDITAR` |
| 4 | `PUT` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}` | Editar un riesgo existente | `PROYECTO:EDITAR` |
| 5 | `GET` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}/soluciones` | Listar soluciones cargadas para un riesgo | `PROYECTO:VER` |
| 6 | `POST` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}/soluciones` | Agregar varias soluciones a un riesgo (multipart) — **201** | `PROYECTO:EDITAR` |
| 7 | `GET` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}/soluciones/{solucionId}/descargar` | Descargar un PDF de solución | `PROYECTO:VER` |
| 8 | `GET` | `/api/v1/proyectos/{proyectoId}/riesgos/descargar-excel` | Descargar la matriz de riesgos en Excel | `PROYECTO:VER` |
| 9 | `GET` | `/api/v1/proyectos/riesgos/matriz` | Obtener la matriz global de riesgos | `hasBaseAccess` |

---

### 25.6 POST `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}/soluciones`

**Content-Type:** `multipart/form-data`

| Campo | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `archivos` | MultipartFile[] | Sí | Uno o más archivos PDF |

**Response 201:** `List<RiesgoSolucionAdjuntoDTO>`

---

## 26. Módulo Jerarquía del Proyecto

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")` (clase)
**Controller:** `ProjectHierarchyController` | **Creado:** 2026-04-29

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `PUT` | `/api/v1/proyectos/{id}/entregables/{entregableId}` | Editar entregable | `canManageProjectStructure` |
| 2 | `POST` | `/api/v1/proyectos/{id}/entregables/{entregableId}/cambiar-descripcion` | Cambiar descripción con justificación y PDF de soporte | `canChangeDescription` |
| 3 | `POST` | `/api/v1/proyectos/{id}/entregables/{entregableId}/cambiar-fecha` | Cambiar fecha límite con justificación y PDF de soporte | `canChangeDeadline` |
| 4 | `GET` | `/api/v1/proyectos/{id}/entregables/{entregableId}/historial-fechas` | Obtener historial de cambios de fecha | `canChangeDeadline` |
| 5 | `GET` | `/api/v1/proyectos/{id}/entregables/cambios-descripcion/{cambioId}/descargar` | Descargar PDF de soporte de un cambio de descripción | `canChangeDescription` |
| 6 | `GET` | `/api/v1/proyectos/{id}/entregables/cambios-fecha/{cambioId}/descargar` | Descargar PDF de soporte de un cambio de fecha | `canChangeDeadline` |
| 7 | `POST` | `/api/v1/proyectos/{id}/fases` | Agregar fase | `canManageProjectStructure` |
| 8 | `PUT` | `/api/v1/proyectos/{id}/fases/{faseId}` | Editar fase | `canManageProjectStructure` |
| 9 | `POST` | `/api/v1/proyectos/{id}/fases/{faseId}/hitos` | Agregar hito | `canManageProjectStructure` |
| 10 | `PUT` | `/api/v1/proyectos/{id}/fases/{faseId}/hitos/{hitoId}` | Editar hito | `canManageProjectStructure` |
| 11 | `POST` | `/api/v1/proyectos/{id}/fases/{faseId}/hitos/{hitoId}/entregables` | Agregar entregable | `canManageProjectStructure` |
| 12 | `GET` | `/api/v1/proyectos/{id}/hierarchy` | Jerarquía completa del proyecto | `PROYECTO:VER` |

---

### 26.3 POST `/api/v1/proyectos/{id}/entregables/{entregableId}/cambiar-fecha`

**Content-Type:** `multipart/form-data`

| Campo | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `request` | JSON | Sí | `CambioFechaRequest` con nueva fecha y justificación |
| `evidencia` | MultipartFile | Sí | Archivo PDF de soporte |

**Response 200:** `CambioFechaResponse`

---

## 27. Módulo Avance y Evidencia

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")` (clase)
**Controller:** `AvanceProyectoController` | **Creado:** 2026-04-24

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `GET` | `/api/v1/proyectos/{proyectoId}/avance` | Obtener el avance detallado del proyecto | `PROYECTO:VER` |
| 2 | `POST` | `/api/v1/proyectos/{proyectoId}/avance/alertar-director` | Enviar alerta de avance al director | `canSendDirectorNotification` |
| 3 | `PATCH` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/aprobar` | Aprobar entregable | `canReviewEvidence` |
| 4 | `POST` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/aprobar` | Aprobar entregable | `canReviewEvidence` |
| 5 | `GET` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/evidencia` | Descargar la evidencia vigente de un entregable | `PROYECTO:VER` |
| 6 | `POST` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/evidencia` | Subir evidencia de entregable (multipart) | `EVIDENCIA:CARGAR` |
| 7 | `GET` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/observaciones` | Listar observaciones documentales de un entregable | `PROYECTO:VER` |
| 8 | `PUT` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/observaciones/{observacionId}/subsanar` | Marcar una observación de evidencia como subsanada | `canMarkEvidenceCorrected` |
| 9 | `PATCH` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/rechazar` | Rechazar entregable | `canReviewEvidence` |
| 10 | `POST` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/rechazar` | Rechazar entregable | `canReviewEvidence` |
| 11 | `GET` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/versiones` | Listar versiones documentales de un entregable | `canViewDocumentHistory` |
| 12 | `POST` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/versiones/{versionId}/revertir` | Revertir a una versión anterior de la evidencia | `canRevertDocumentVersion` |

---

### 27.6 POST `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/evidencia`

**Content-Type:** `multipart/form-data`

| Campo | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `fechaEntrega` | date (ISO) | Sí | Fecha de entrega |
| `evidencia` | MultipartFile | Sí | Archivo PDF de evidencia |

**Response 200:** `EntregableAprobadoResponseDTO`

---

### 27.12 POST `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/versiones/{versionId}/revertir`

**Body (opcional):**
```json
{ "motivo": "La versión anterior era la correcta" }
```

**Response 200:** `EntregableAprobadoResponseDTO`

---

## 28. Módulo Dashboard

**Base path:** `/api/v1/dashboard`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(...)")` (clase) · `@proyectoSecurity.canViewDashboard(...)` (método)
**Controller:** `DashboardController` | **Creado:** 2026-04-23

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `GET` | `/api/v1/dashboard/avance-por-proyecto` | Lista de proyectos con su avance — **204** si no hay proyectos |
| 2 | `GET` | `/api/v1/dashboard/kpis` | Resumen ejecutivo (KPIs globales) |

---

## 29. Módulo Proyectos

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")` (clase)
**Controller:** `ProyectoController` | **Creado:** 2026-04-23

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 1 | `GET` | `/api/v1/proyectos` | Listar proyectos (con filtros y paginación) | `PROYECTO:VER` (global) |
| 2 | `GET` | `/api/v1/proyectos/{id}` | Obtener detalle del proyecto | `PROYECTO:VER` |
| 3 | `PUT` | `/api/v1/proyectos/{id}` | Actualizar proyecto | `PROYECTO:EDITAR` |
| 4 | `PATCH` | `/api/v1/proyectos/{id}/cierre-forzoso` | Cierre forzoso / extraordinario | `canForceCloseExtraordinary` |
| 5 | `PUT` | `/api/v1/proyectos/{id}/completar-informacion` | Completar información inicial | `canCompleteInitialRegistration` |
| 6 | `GET` | `/api/v1/proyectos/{id}/completion-status` | Estado de completitud | `PROYECTO:VER` |
| 7 | `GET` | `/api/v1/proyectos/{id}/completitud-borrador` | Obtener borrador de completitud | `canCompleteInitialRegistration` |
| 8 | `PATCH` | `/api/v1/proyectos/{id}/completitud-borrador` | Guardar borrador de completitud | `canCompleteInitialRegistration` |
| 9 | `PATCH` | `/api/v1/proyectos/{id}/completitud-fase/{fase}` | Completar fase | `canCompleteInitialRegistration` |
| 10 | `GET` | `/api/v1/proyectos/{id}/furag` | Obtener FURAG | `PROYECTO:VER` |
| 11 | `PUT` | `/api/v1/proyectos/{id}/furag` | Actualizar FURAG | `PROYECTO:EDITAR` |
| 12 | `GET` | `/api/v1/proyectos/{id}/resumen` | Resumen para cierre | `PROYECTO:VER` |
| 13 | `GET` | `/api/v1/proyectos/directores-asignables` | Listar directores asignables | `PROYECTO:CREAR` (global) |
| 14 | `GET` | `/api/v1/proyectos/mis-proyectos` | Listar mis proyectos | acceso a proyectos propios |
| 15 | `POST` | `/api/v1/proyectos/registro-inicial` | Registro inicial de proyecto — **201** | `PROYECTO:CREAR` (global) |
| 16 | `GET` | `/api/v1/proyectos/siguiente-codigo` | Obtener siguiente código | `PROYECTO:CREAR` (global) |

---

### 29.1 GET `/api/v1/proyectos`

**Query params:**
| Param | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `nombre` | string | No | Filtrar por nombre (parcial) |
| `codigo` | string | No | Filtrar por código |
| `dependencia` | string | No | Filtrar por dependencia |
| `estado` | string | No | Filtrar por estado |
| `peti` | boolean | No | Filtrar proyectos PETI |
| `page` | integer | No | Página (default: 0) |
| `size` | integer | No | Tamaño (default: 10) |
| `sort` | string | No | Ordenamiento (default: `id,DESC`) |

**Response 200:** `Page<ProyectoListDTO>`

---

### 29.15 POST `/api/v1/proyectos/registro-inicial`

**Body:**
```json
{
  "nombre": "Proyecto de Prueba",
  "dependencia": "Secretaría TIC",
  "directorId": 5
}
```

**Response 201:** `ProyectoCreatedDTO`

---

## Códigos de Error

| Código HTTP | Significado                          | Acción recomendada                         |
| ----------- | ------------------------------------ | ------------------------------------------ |
| `200`       | Operación exitosa                    | —                                          |
| `201`       | Recurso creado exitosamente          | —                                          |
| `204`       | Sin contenido (borrado o lista vacía)| —                                          |
| `400`       | Solicitud inválida                   | Revisar parámetros y body                  |
| `401`       | No autenticado                       | Verificar token JWT (`action: REFRESH_OR_LOGIN`) |
| `403`       | Sin permisos                         | Verificar permisos/rol en Keycloak         |
| `404`       | Recurso no encontrado                | Verificar ID/ruta o firma HMAC del enlace público |
| `413`       | Archivo demasiado grande             | Reducir tamaño del archivo (máx. 50 MB)    |
| `415`       | Tipo de archivo no soportado         | Verificar formato aceptado                 |
| `422`       | Entidad no procesable                | Verificar formato de datos                 |
| `429`       | Límite de solicitudes excedido       | Esperar `Retry-After` segundos y reintentar|
| `500`       | Error interno del servidor           | Contactar equipo de soporte                |

### Formato de Error Estándar

Todos los errores (salvo el 401) siguen **Problem Details, RFC 9457** — ver [Convenciones → Formato de error](#formato-de-error):

```json
{
  "type": "/errors/bad-request",
  "title": "Solicitud inválida",
  "status": 400,
  "detail": "Error de validación: nombre: el nombre es obligatorio"
}
```

El **401** usa el formato propio con `action: "REFRESH_OR_LOGIN"` (ver *Convenciones → Formato de error*).

---

## Modelos de Datos

### ApiResponse\<T\>

| Campo     | Tipo  | Descripción                     |
| --------- | ----- | ------------------------------- |
| `success` | bool  | Indica si la operación fue exitosa |
| `message` | string| Mensaje descriptivo              |
| `data`    | T     | Datos de la respuesta            |

### ProyectoListDTO

| Campo         | Tipo    | Descripción                  |
| ------------- | ------- | ---------------------------- |
| `id`          | string  | ID del proyecto              |
| `nombre`      | string  | Nombre del proyecto          |
| `codigo`      | string  | Código secuencial            |
| `dependencia` | string  | Dependencia responsable      |
| `estado`      | string  | Estado del proyecto          |
| `peti`        | boolean | ¿Es proyecto PETI?           |
| `avance`      | number  | Porcentaje de avance         |

### EntregableAprobadoResponseDTO

| Campo            | Tipo    | Descripción                    |
| ---------------- | ------- | ------------------------------ |
| `entregableId`   | int     | ID del entregable              |
| `estado`         | string  | Estado de aprobación           |
| `versionActual`  | int     | Número de versión actual       |
| `fechaEntrega`   | date    | Fecha de entrega               |

### CierreProyectoResponse

| Campo           | Tipo    | Descripción                     |
| --------------- | ------- | ------------------------------- |
| `proyectoId`    | string  | ID del proyecto                 |
| `estado`        | string  | Estado del cierre               |
| `fechaCierre`   | datetime| Fecha de cierre                 |
| `mensaje`       | string  | Mensaje descriptivo             |

---

*Documento actualizado el 2026-09-29 con los 172 endpoints reales del sistema (29 controladores), ordenados del más reciente al más antiguo por fecha de creación del controlador.*
