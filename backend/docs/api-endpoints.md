# Documentación de Endpoints API — api-gestion (Proyecta)

| Campo              | Valor                                      |
| ------------------ | ------------------------------------------ |
| **Proyecto**       | api-gestion (Proyecta)                     |
| **Base URL**       | `http://localhost:8080`                     |
| **Autenticación**  | OAuth2 Bearer Token (JWT via Keycloak)     |
| **Versión API**    | v1                                         |
| **Total Endpoints**| **150**                                    |

> **Orden:** Módulos ordenados del más reciente al más antiguo según fecha de incorporación al repositorio.

---

## Índice

| # | Módulo | Controllers | Fecha creación | Endpoints |
| --- | ------ | ----------- | -------------- | --------- |
| 1 | [Permisos de Usuario](#1-módulo-permisos-de-usuario) | `PermisoUsuarioController` | 2026-07-23 | 2 |
| 2 | [Listas Paramétricas](#2-módulo-listas-paramétricas) | `ListaParametricaController` | 2026-07-23 | 7 |
| 3 | [Preguntas de Cierre](#3-módulo-preguntas-de-cierre) | `ClosureQuestionController` | 2026-07-09 | 12 |
| 4 | [Plantillas de Cierre](#4-módulo-plantillas-de-cierre) | `ClosureTemplateController` | 2026-07-09 | 5 |
| 5 | [Indicadores de Eficiencia](#5-módulo-indicadores-de-eficiencia) | `IndicadorEficienciaController` | 2026-07-06 | 1 |
| 6 | [Administración de Notificaciones](#6-módulo-administración-de-notificaciones) | `NotificationAdminController` | 2026-06-18 | 9 |
| 7 | [Notificaciones In-App](#7-módulo-notificaciones-in-app) | `InAppNotificationController` | 2026-06-18 | 4 |
| 8 | [Beneficio e Impacto](#8-módulo-beneficio-e-impacto) | `ProyectoBeneficioImpactoController` | 2026-06-18 | 3 |
| 9 | [Catálogos de Configuración](#9-módulo-catálogos-de-configuración) | `ConfiguracionCatalogoController` | 2026-06-11 | 1 |
| 10 | [Analytics](#10-módulo-analytics) | `AnalyticsController` | 2026-05-28 | 2 |
| 11 | [Administración de Seguridad](#11-módulo-administración-de-seguridad) | `AdminConfiguracionController` | 2026-05-26 | 13 |
| 12 | [Autorización](#12-módulo-autorización) | `AuthorizationController` | 2026-05-26 | 2 |
| 13 | [Administración de Ponderaciones](#13-módulo-administración-de-ponderaciones) | `PonderacionAdministracionController` | 2026-05-21 | 4 |
| 14 | [Usuarios](#14-módulo-usuarios) | `UsuarioController` | 2026-05-11 | 2 |
| 15 | [Documentos del Proyecto](#15-módulo-documentos-del-proyecto) | `DocumentoController` | 2026-05-06 | 5 |
| 16 | [Configuración de Reportes (Admin)](#16-módulo-configuración-de-reportes-admin) | `ReporteConfigController` | 2026-05-05 | 2 |
| 17 | [Reportes](#17-módulo-reportes) | `ReporteController` | 2026-05-05 | 13 |
| 18 | [Cronograma](#18-módulo-cronograma) | `CronogramaController` | 2026-05-05 | 3 |
| 19 | [Cierre de Proyecto](#19-módulo-cierre-de-proyecto) | `ProjectClosureController` | 2026-05-04 | 5 |
| 20 | [Riesgos](#20-módulo-riesgos) | `RiesgoController` | 2026-04-30 | 9 |
| 21 | [Jerarquía del Proyecto](#21-módulo-jerarquía-del-proyecto) | `ProjectHierarchyController` | 2026-04-29 | 14 |
| 22 | [Avance y Evidencia](#22-módulo-avance-y-evidencia) | `AvanceProyectoController` | 2026-04-24 | 10 |
| 23 | [Evidencia Pública](#23-módulo-evidencia-pública) | `PublicEvidenceController` | 2026-04-24* | 1 |
| 24 | [Dashboard](#24-módulo-dashboard) | `DashboardController` | 2026-04-23 | 4 |
| 25 | [Proyectos](#25-módulo-proyectos) | `ProyectoController` | 2026-04-23 | 17 |

---

## Convenciones

### Formato de Request/Response

- Content-Type: `application/json` (salvo endpoints multipart)
- Codificación: UTF-8
- Fechas: ISO 8601 (`2026-07-29T14:30:00Z`)
- UUIDs: RFC 4122 (`550e8400-e29b-41d4-a716-446655440000`)
- Respuestas estándar envueltas en `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Descripción del resultado",
  "data": { ... }
}
```

### Autenticación

Todos los endpoints requieren header `Authorization` (excepto los marcados como públicos):

```
Authorization: Bearer <jwt_token>
```

El token debe contener el rol `app_access` en el claim `realm_access.roles`.

### Paginación

| Parámetro   | Tipo    | Default | Descripción                    |
| ----------- | ------- | ------- | ------------------------------ |
| `page`      | integer | 0       | Número de página (0-indexed)   |
| `size`      | integer | 10      | Tamaño de página (máx. 100)    |
| `sort`      | string  | `id`    | Campo y dirección de orden     |

### Control de Acceso

| Prefijo de URL              | Permiso requerido                     | Rol mínimo        |
| --------------------------- | ------------------------------------- | ----------------- |
| `/api/v1/proyectos`         | `PROYECTO:VER`, `PROYECTO:EDITAR`, etc. | `app_access`  |
| `/api/v1/admin/...`         | `SISTEMA:CONFIGURAR`                  | `ADMINISTRADOR`   |
| `/api/v1/configuracion/...` | `SISTEMA:CONFIGURAR` (escritura)      | `app_access`      |
| `/api/v1/public/...`        | **Sin autenticación**                 | —                 |

---

## 1. Módulo Permisos de Usuario

**Base path:** `/api/v1/admin/configuracion/permisos-usuario`
**Auth:** `@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")`
**Controller:** `PermisoUsuarioController` | **Creado:** 2026-07-23

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 120 | `GET` | `/api/v1/admin/configuracion/permisos-usuario/{usuarioId}` | Obtener matriz de permisos del usuario |
| 121 | `PUT` | `/api/v1/admin/configuracion/permisos-usuario` | Guardar matriz de permisos del usuario |

---

## 2. Módulo Listas Paramétricas

**Base path:** `/api/v1/configuracion/listas`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `ListaParametricaController` | **Creado:** 2026-07-23

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 95 | `GET` | `/api/v1/configuracion/listas/{listaClave}` | Listar items de una lista | — |
| 96 | `GET` | `/api/v1/configuracion/listas/{listaClave}/valores` | Listar valores activos | — |
| 97 | `GET` | `/api/v1/configuracion/listas/metadata/all` | Listar todos los metadatas | — |
| 98 | `POST` | `/api/v1/configuracion/listas` | Crear/actualizar item | `SISTEMA:CONFIGURAR` |
| 99 | `PUT` | `/api/v1/configuracion/listas/{listaClave}/valores` | Guardar valores de una lista | `SISTEMA:CONFIGURAR` |
| 100 | `DELETE` | `/api/v1/configuracion/listas/{id}` | Eliminar item | `SISTEMA:CONFIGURAR` |
| 101 | `PATCH` | `/api/v1/configuracion/listas/{id}/toggle` | Activar/desactivar item | `SISTEMA:CONFIGURAR` |

---

## 3. Módulo Preguntas de Cierre

**Base path:** `/api/v1/admin/closure-questions`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `ClosureQuestionController` | **Creado:** 2026-07-09

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 138 | `GET` | `/api/v1/admin/closure-questions` | Listar todas las preguntas | `SISTEMA:CONFIGURAR` |
| 139 | `GET` | `/api/v1/admin/closure-questions/active` | Listar preguntas activas | `hasBaseAccess` |
| 140 | `GET` | `/api/v1/admin/closure-questions/{id}` | Obtener pregunta por ID | `SISTEMA:CONFIGURAR` |
| 141 | `POST` | `/api/v1/admin/closure-questions` | Crear pregunta | `SISTEMA:CONFIGURAR` |
| 142 | `PATCH` | `/api/v1/admin/closure-questions/{id}` | Actualizar pregunta | `SISTEMA:CONFIGURAR` |
| 143 | `PATCH` | `/api/v1/admin/closure-questions/{id}/toggle` | Activar/desactivar pregunta | `SISTEMA:CONFIGURAR` |
| 144 | `DELETE` | `/api/v1/admin/closure-questions/{id}` | Eliminar pregunta | `SISTEMA:CONFIGURAR` |
| 145 | `GET` | `/api/v1/admin/closure-questions/answers/{projectId}` | Obtener respuestas de un proyecto | `hasBaseAccess` |
| 146 | `POST` | `/api/v1/admin/closure-questions/answers/{projectId}` | Guardar respuestas | `hasBaseAccess` |
| 147 | `GET` | `/api/v1/admin/closure-questions/resolved-template/{projectId}` | Obtener plantilla resuelta | `hasBaseAccess` |
| 148 | `GET` | `/api/v1/admin/closure-questions/draft/{projectId}` | Obtener borrador de plantilla | `hasBaseAccess` |
| 149 | `GET` | `/api/v1/admin/closure-questions/missing/{projectId}` | Obtener preguntas faltantes | `hasBaseAccess` |

---

### 3.146 POST `/api/v1/admin/closure-questions/answers/{projectId}`

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

## 4. Módulo Plantillas de Cierre

**Base path:** `/api/v1/admin/closure-templates`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `ClosureTemplateController` | **Creado:** 2026-07-09

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 133 | `GET` | `/api/v1/admin/closure-templates/active` | Obtener plantilla activa | `SISTEMA:CONFIGURAR` |
| 134 | `POST` | `/api/v1/admin/closure-templates` | Guardar plantilla | `SISTEMA:CONFIGURAR` |
| 135 | `GET` | `/api/v1/admin/closure-templates/preview` | Previsualizar plantilla (PDF) | `SISTEMA:CONFIGURAR` |
| 136 | `GET` | `/api/v1/admin/closure-templates/closure-record/{projectId}` | Obtener registro de cierre | `hasBaseAccess` |
| 137 | `POST` | `/api/v1/admin/closure-templates/closure-record/{projectId}` | Guardar registro de cierre | `hasBaseAccess` |

---

## 5. Módulo Indicadores de Eficiencia

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `IndicadorEficienciaController` | **Creado:** 2026-07-06

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 69 | `GET` | `/api/v1/proyectos/{proyectoId}/indicadores-eficiencia` | Calcular indicadores de eficiencia | `PROYECTO:VER` |

---

### 5.69 GET `/api/v1/proyectos/{proyectoId}/indicadores-eficiencia`

**Query params:**
| Param | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `fechaCorte` | date (ISO) | Sí | Fecha de corte para el cálculo |

**Response 200:** `IndicadoresEficienciaDTO`

---

## 6. Módulo Administración de Notificaciones

**Base path:** `/api/v1/admin/notificaciones`
**Auth:** `@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")`
**Controller:** `NotificationAdminController` | **Creado:** 2026-06-18

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 124 | `GET` | `/api/v1/admin/notificaciones/eventos` | Listar eventos de notificación |
| 125 | `GET` | `/api/v1/admin/notificaciones/plantillas` | Listar plantillas de notificación |
| 126 | `PUT` | `/api/v1/admin/notificaciones/plantillas` | Guardar plantilla |
| 127 | `GET` | `/api/v1/admin/notificaciones/preferencias` | Listar preferencias |
| 128 | `PUT` | `/api/v1/admin/notificaciones/preferencias` | Guardar preferencia |
| 129 | `POST` | `/api/v1/admin/notificaciones/plantillas/preview` | Previsualizar plantilla |
| 130 | `POST` | `/api/v1/admin/notificaciones/plantillas/test-send` | Enviar correo de prueba |
| 131 | `GET` | `/api/v1/admin/notificaciones/estadisticas` | Estadísticas de notificaciones |
| 132 | `GET` | `/api/v1/admin/notificaciones/diagnostico` | Diagnóstico del sistema de correo |

---

### 6.130 POST `/api/v1/admin/notificaciones/plantillas/test-send`

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
    "recipient": "juan**@domain.com",
    "status": "SENT",
    "success": true
  }
}
```

---

## 7. Módulo Notificaciones In-App

**Base path:** `/api/v1/notificaciones`
**Auth:** Autenticado (implícito)
**Controller:** `InAppNotificationController` | **Creado:** 2026-06-18

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 91 | `GET` | `/api/v1/notificaciones` | Listar notificaciones (paginado) |
| 92 | `GET` | `/api/v1/notificaciones/no-leidas` | Conteo de notificaciones no leídas |
| 93 | `PATCH` | `/api/v1/notificaciones/{id}/leer` | Marcar notificación como leída |
| 94 | `PATCH` | `/api/v1/notificaciones/marcar-todas-leidas` | Marcar todas como leídas |

---

### 7.91 GET `/api/v1/notificaciones`

**Query params:**
| Param | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `leido` | boolean | No | Filtrar por estado de lectura |
| `page` | integer | No | Página |
| `size` | integer | No | Tamaño |

**Response 200:** `Page<InAppNotificationDTO>`

---

## 8. Módulo Beneficio e Impacto

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `ProyectoBeneficioImpactoController` | **Creado:** 2026-06-18

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 63 | `GET` | `/api/v1/proyectos/{proyectoId}/beneficio-impacto` | Obtener información de beneficio e impacto | `canViewBenefitImpact` |
| 64 | `PUT` | `/api/v1/proyectos/{proyectoId}/beneficio-impacto` | Guardar información | `canEditBenefitImpact` |
| 65 | `POST` | `/api/v1/proyectos/{proyectoId}/beneficio-impacto/revision` | Revisar/aprobar información | `hasBaseAccess` |

---

### 8.65 POST `/api/v1/proyectos/{proyectoId}/beneficio-impacto/revision`

**Query params:**
| Param | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `aprobado` | boolean | Sí | true para aprobar, false para observar |
| `observaciones` | string | No | Observaciones en caso de rechazo |

---

## 9. Módulo Catálogos de Configuración

**Base path:** `/api/v1/configuracion/catalogos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `ConfiguracionCatalogoController` | **Creado:** 2026-06-11

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 102 | `GET` | `/api/v1/configuracion/catalogos/peti` | Obtener catálogo PETI |

---

## 10. Módulo Analytics

**Base path:** `/api/v1/analytics`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `AnalyticsController` | **Creado:** 2026-05-28

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 87 | `GET` | `/api/v1/analytics/portafolio` | Analítica del portafolio | `PROYECTO:VER` |
| 88 | `GET` | `/api/v1/analytics/portafolio/pdf` | Descargar analítica del portafolio (PDF) | `PROYECTO:VER` |

---

## 11. Módulo Administración de Seguridad

**Base path:** `/api/v1/admin/configuracion`
**Auth:** `@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")`
**Controller:** `AdminConfiguracionController` | **Creado:** 2026-05-26

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 107 | `GET` | `/api/v1/admin/configuracion/usuarios` | Listar usuarios (paginado, con filtros) |
| 108 | `PUT` | `/api/v1/admin/configuracion/usuarios` | Actualizar usuario |
| 109 | `GET` | `/api/v1/admin/configuracion/roles` | Listar roles activos |
| 110 | `GET` | `/api/v1/admin/configuracion/roles/todos` | Listar todos los roles |
| 111 | `POST` | `/api/v1/admin/configuracion/roles` | Crear rol |
| 112 | `PUT` | `/api/v1/admin/configuracion/roles/{codigo}` | Actualizar rol |
| 113 | `DELETE` | `/api/v1/admin/configuracion/roles/{codigo}` | Desactivar rol |
| 114 | `GET` | `/api/v1/admin/configuracion/permisos` | Listar permisos disponibles |
| 115 | `GET` | `/api/v1/admin/configuracion/cargos-asignacion` | Listar cargos de asignación |
| 116 | `PUT` | `/api/v1/admin/configuracion/roles-permisos` | Actualizar matriz de permisos |
| 117 | `POST` | `/api/v1/admin/configuracion/usuario-proyecto` | Asignar usuario a proyecto |
| 118 | `GET` | `/api/v1/admin/configuracion/usuario-proyecto` | Listar asignaciones de un usuario |
| 119 | `GET` | `/api/v1/admin/configuracion/storage-path/test` | Validar ruta de almacenamiento |

---

### 11.107 GET `/api/v1/admin/configuracion/usuarios`

**Query params:**
| Param | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `search` | string | No | Búsqueda por nombre/username |
| `rol` | string | No | Filtrar por rol |
| `page` | integer | No | Página |
| `size` | integer | No | Tamaño |

**Response 200:** `Page<SeguridadUsuarioDTO>`

---

### 11.116 PUT `/api/v1/admin/configuracion/roles-permisos`

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

## 12. Módulo Autorización

**Base path:** `/api/v1/authz`
**Auth:** `@PreAuthorize("isAuthenticated()")`
**Controller:** `AuthorizationController` | **Creado:** 2026-05-26

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 89 | `GET` | `/api/v1/authz/me` | Información de autorización del usuario actual |
| 90 | `GET` | `/api/v1/authz/role-aliases` | Alias de roles para el cliente |

---

## 13. Módulo Administración de Ponderaciones

**Base path:** `/api/v1/admin/ponderaciones`
**Auth:** `@PreAuthorize(... hasBaseAccess ... and hasAnyRole('ADMINISTRADOR'))`
**Controller:** `PonderacionAdministracionController` | **Creado:** 2026-05-21

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 103 | `GET` | `/api/v1/admin/ponderaciones/auditoria/{proyectoId}` | Auditar consistencia de ponderaciones |
| 104 | `POST` | `/api/v1/admin/ponderaciones/normalizar/{proyectoId}` | Normalizar ponderaciones de un proyecto |
| 105 | `POST` | `/api/v1/admin/ponderaciones/normalizar-todos` | Normalizar todos los proyectos |
| 106 | `POST` | `/api/v1/admin/ponderaciones/recalcular/{proyectoId}` | Recalcular avances |

---

## 14. Módulo Usuarios

**Base path:** `/api/v1/usuarios`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `UsuarioController` | **Creado:** 2026-05-11

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 1 | `GET` | `/api/v1/usuarios/me` | Obtener perfil del usuario autenticado |
| 2 | `PATCH` | `/api/v1/usuarios/me/notificaciones-globales` | Activar/desactivar notificaciones globales |

---

### 14.1 GET `/api/v1/usuarios/me`

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

### 14.2 PATCH `/api/v1/usuarios/me/notificaciones-globales`

**Body:**
```json
{ "recibirNotificacionesGlobales": true }
```

---

## 15. Módulo Documentos del Proyecto

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `DocumentoController` | **Creado:** 2026-05-06

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 58 | `GET` | `/api/v1/proyectos/{proyectoId}/documentos` | Listar documentos del proyecto | `PROYECTO:VER` |
| 59 | `POST` | `/api/v1/proyectos/{proyectoId}/documentos/{tipoDocumento}` | Cargar documento (multipart) | `DOCUMENTO:CARGAR` |
| 60 | `GET` | `/api/v1/proyectos/{proyectoId}/documentos/{tipoDocumento}/descargar` | Descargar documento | `PROYECTO:VER` |
| 61 | `GET` | `/api/v1/proyectos/{proyectoId}/documentos/{tipoDocumento}/versiones` | Historial de versiones | `PROYECTO:VER` |
| 62 | `GET` | `/api/v1/proyectos/{proyectoId}/documentos/{tipoDocumento}/versiones/{numeroVersion}/archivo` | Descargar versión específica | `PROYECTO:VER` |

---

### 15.59 POST `/api/v1/proyectos/{proyectoId}/documentos/{tipoDocumento}`

**Content-Type:** `multipart/form-data`

| Campo | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `archivo` | MultipartFile | Sí | Archivo del documento |
| `observacion` | string | No | Observación opcional |

**Response 201:** `DocumentoUploadResultDTO`

---

## 16. Módulo Configuración de Reportes (Admin)

**Base path:** `/api/configuracion/reportes` (sin `/v1`)
**Auth:** `@PreAuthorize(... hasBaseAccess ... and hasAnyRole('ADMINISTRADOR'))`
**Controller:** `ReporteConfigController` | **Creado:** 2026-05-05

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 122 | `GET` | `/api/configuracion/reportes` | Listar configuraciones de reportes |
| 123 | `PATCH` | `/api/configuracion/reportes/{id}` | Actualizar configuración de reporte |

---

## 17. Módulo Reportes

**Base path:** `/api/v1/reportes`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `ReporteController` | **Creado:** 2026-05-05

| # | Método | Endpoint | Descripción | Permiso | Formato |
| --- | ------ | -------- | ----------- | ------- | ------- |
| 70 | `GET` | `/api/v1/reportes/configuracion` | Configuración de reportes | `PROYECTO:VER` | JSON |
| 71 | `GET` | `/api/v1/reportes/vista-previa/{proyectoId}` | Vista previa de reporte | `PROYECTO:VER` | JSON |
| 72 | `GET` | `/api/v1/reportes/todos-los-proyectos` | Reporte de todos los proyectos | `PROYECTO:VER` | JSON |
| 73 | `GET` | `/api/v1/reportes/proyectos-con-retrasos` | Proyectos con retrasos | `PROYECTO:VER` | JSON |
| 74 | `GET` | `/api/v1/reportes/furag/{proyectoId}` | Reporte FURAG | `PROYECTO:VER` | JSON |
| 75 | `GET` | `/api/v1/reportes/riesgos` | Verificación de tratamiento a riesgos | `PROYECTO:VER` | JSON |
| 76 | `GET` | `/api/v1/reportes/proyecto/{id}/descargar` | Descargar reporte de proyecto (PDF) | `PROYECTO:VER` | PDF |
| 77 | `GET` | `/api/v1/reportes/portafolio/descargar` | Descargar reporte de portafolio (PDF) | `PROYECTO:VER` | PDF |
| 78 | `GET` | `/api/v1/reportes/proyectos-con-retrasos/descargar` | Descargar reporte de retrasos (PDF) | `PROYECTO:VER` | PDF |
| 79 | `GET` | `/api/v1/reportes/plan-comunicaciones/descargar` | Descargar plan de comunicaciones (PDF) | `PROYECTO:VER` | PDF |
| 80 | `GET` | `/api/v1/reportes/furag/{proyectoId}/descargar` | Descargar reporte FURAG (PDF) | `PROYECTO:VER` | PDF |
| 81 | `GET` | `/api/v1/reportes/riesgos/descargar` | Descargar reporte de riesgos (PDF) | `PROYECTO:VER` | PDF |
| 82 | `GET` | `/api/v1/reportes/portafolio/excel` | Descargar portafolio (Excel) | `PROYECTO:VER` (propios) | XLSX |

---

### 17.82 GET `/api/v1/reportes/portafolio/excel`

**Query params:**
| Param | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `query` | string | No | Búsqueda por nombre |
| `dependency` | string | No | Filtrar por dependencia |
| `status` | string | No | Filtrar por estado |
| `peti` | string | No | Filtrar por PETI |

---

## 18. Módulo Cronograma

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `CronogramaController` | **Creado:** 2026-05-05

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 66 | `GET` | `/api/v1/proyectos/{proyectoId}/cronograma` | Obtener cronograma | `PROYECTO:VER` |
| 67 | `POST` | `/api/v1/proyectos/{proyectoId}/cronograma` | Cargar cronograma (PDF multipart) | `CRONOGRAMA:CARGAR` |
| 68 | `GET` | `/api/v1/proyectos/{proyectoId}/cronograma/descargar` | Descargar cronograma (PDF) | `PROYECTO:VER` |

---

## 19. Módulo Cierre de Proyecto

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `ProjectClosureController` | **Creado:** 2026-05-04

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 43 | `POST` | `/api/v1/proyectos/{id}/cierre` | Cerrar proyecto directamente | `PROYECTO:CERRAR` |
| 44 | `POST` | `/api/v1/proyectos/{id}/cierre/solicitar` | Solicitar cierre (workflow) | `CIERRE:SOLICITAR` |
| 45 | `POST` | `/api/v1/proyectos/{id}/cierre/aprobar` | Aprobar cierre | `CIERRE:APROBAR` |
| 46 | `POST` | `/api/v1/proyectos/{id}/cierre/rechazar` | Rechazar cierre | `CIERRE:APROBAR` |
| 47 | `GET` | `/api/v1/proyectos/{id}/cierre/descargar` | Descargar acta de cierre (DOCX/PDF) | `PROYECTO:VER` |

---

### 19.44 POST `/api/v1/proyectos/{id}/cierre/solicitar`

**Body:**
```json
{
  "observaciones": "Proyecto cumplió todos los entregables",
  "fechaCierre": "2026-07-29"
}
```

**Response 200:** `CierreProyectoResponse`

---

### 19.46 POST `/api/v1/proyectos/{id}/cierre/rechazar`

**Body:**
```json
{ "observaciones": "Faltan entregables por revisar" }
```

---

## 20. Módulo Riesgos

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `RiesgoController` | **Creado:** 2026-04-30

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 34 | `GET` | `/api/v1/proyectos/{proyectoId}/riesgos` | Listar riesgos del proyecto | `PROYECTO:VER` |
| 35 | `GET` | `/api/v1/proyectos/riesgos/matriz` | Obtener matriz de riesgos global | `hasBaseAccess` |
| 36 | `POST` | `/api/v1/proyectos/{proyectoId}/riesgos` | Crear riesgo | `PROYECTO:EDITAR` |
| 37 | `PUT` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}` | Actualizar riesgo | `PROYECTO:EDITAR` |
| 38 | `DELETE` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}` | Eliminar riesgo | `PROYECTO:EDITAR` |
| 39 | `PATCH` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}/tratamiento` | Verificar tratamiento | `PROYECTO:EDITAR` |
| 40 | `GET` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}/soluciones` | Listar soluciones adjuntas | `PROYECTO:VER` |
| 41 | `POST` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}/soluciones` | Agregar soluciones (archivos) | `PROYECTO:EDITAR` |
| 42 | `GET` | `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}/soluciones/{solucionId}/descargar` | Descargar solución (PDF) | `PROYECTO:VER` |

---

### 20.41 POST `/api/v1/proyectos/{proyectoId}/riesgos/{riesgoId}/soluciones`

**Content-Type:** `multipart/form-data`

| Campo | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `archivos` | MultipartFile[] | Sí | Uno o más archivos PDF |

**Response 201:** `List<RiesgoSolucionAdjuntoDTO>`

---

## 21. Módulo Jerarquía del Proyecto

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `ProjectHierarchyController` | **Creado:** 2026-04-29

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 20 | `GET` | `/api/v1/proyectos/{id}/hierarchy` | Obtener jerarquía completa | `PROYECTO:VER` |
| 21 | `POST` | `/api/v1/proyectos/{id}/fases` | Agregar fase | `canManageProjectStructure` |
| 22 | `PUT` | `/api/v1/proyectos/{id}/fases/{faseId}` | Editar fase | `canManageProjectStructure` |
| 23 | `DELETE` | `/api/v1/proyectos/{id}/fases/{faseId}` | Eliminar fase | `canManageProjectStructure` |
| 24 | `POST` | `/api/v1/proyectos/{id}/fases/{faseId}/hitos` | Agregar hito | `canManageProjectStructure` |
| 25 | `PUT` | `/api/v1/proyectos/{id}/fases/{faseId}/hitos/{hitoId}` | Editar hito | `canManageProjectStructure` |
| 26 | `DELETE` | `/api/v1/proyectos/{id}/fases/{faseId}/hitos/{hitoId}` | Eliminar hito | `canManageProjectStructure` |
| 27 | `GET` | `/api/v1/proyectos/{id}/entregables` | Listar todos los entregables | `PROYECTO:VER` |
| 28 | `POST` | `/api/v1/proyectos/{id}/fases/{faseId}/hitos/{hitoId}/entregables` | Agregar entregable | `canManageProjectStructure` |
| 29 | `PUT` | `/api/v1/proyectos/{id}/entregables/{entregableId}` | Editar entregable | `canManageProjectStructure` |
| 30 | `DELETE` | `/api/v1/proyectos/{id}/entregables/{entregableId}` | Eliminar entregable | `canManageProjectStructure` |
| 31 | `POST` | `/api/v1/proyectos/{id}/entregables/{entregableId}/cambiar-fecha` | Cambiar fecha límite (con evidencia) | `canChangeDeadline` |
| 32 | `GET` | `/api/v1/proyectos/{id}/entregables/{entregableId}/historial-fechas` | Historial de cambios de fecha | `canChangeDeadline` |
| 33 | `GET` | `/api/v1/proyectos/{id}/entregables/cambios-fecha/{cambioId}/descargar` | Descargar PDF de justificación | `canChangeDeadline` |

---

### 21.31 POST `/api/v1/proyectos/{id}/entregables/{entregableId}/cambiar-fecha`

**Content-Type:** `multipart/form-data`

| Campo | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `request` | JSON | Sí | `CambioFechaRequest` con nueva fecha y justificación |
| `evidencia` | MultipartFile | Sí | Archivo PDF de soporte |

**Response 200:** `CambioFechaResponse`

---

## 22. Módulo Avance y Evidencia

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `AvanceProyectoController` | **Creado:** 2026-04-24

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 48 | `GET` | `/api/v1/proyectos/{proyectoId}/avance` | Obtener avance detallado del proyecto | `PROYECTO:VER` |
| 49 | `POST` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/evidencia` | Registrar evidencia (multipart) | `EVIDENCIA:CARGAR` |
| 50 | `PATCH` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/aprobar` | Aprobar entregable | `canReviewEvidence` |
| 51 | `PATCH` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/rechazar` | Rechazar entregable | `canReviewEvidence` |
| 52 | `GET` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/versiones` | Listar versiones documentales | `canViewDocumentHistory` |
| 53 | `GET` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/observaciones` | Listar observaciones | `PROYECTO:VER` |
| 54 | `PUT` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/observaciones/{observacionId}/subsanar` | Marcar observación como subsanada | `canMarkEvidenceCorrected` |
| 55 | `POST` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/versiones/{versionId}/revertir` | Revertir a versión anterior | `canRevertDocumentVersion` |
| 56 | `GET` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/versiones/{versionId}/archivo` | Descargar archivo de versión | `canViewDocumentHistory` |
| 57 | `GET` | `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/evidencia` | Descargar evidencia actual (PDF) | `PROYECTO:VER` |

---

### 22.49 POST `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/evidencia`

**Content-Type:** `multipart/form-data`

| Campo | Tipo | Obligatorio | Descripción |
| ----- | ---- | ----------- | ----------- |
| `fechaEntrega` | date (ISO) | Sí | Fecha de entrega |
| `evidencia` | MultipartFile | Sí | Archivo PDF de evidencia |

**Response 200:** `EntregableAprobadoResponseDTO`

---

### 22.55 POST `/api/v1/proyectos/{proyectoId}/avance/entregables/{entregableId}/versiones/{versionId}/revertir`

**Body (opcional):**
```json
{ "motivo": "La versión anterior era la correcta" }
```

**Response 200:** `EntregableAprobadoResponseDTO`

---

## 23. Módulo Evidencia Pública

**Base path:** `/api/v1/public`
**Auth:** **Sin autenticación** (endpoint público)
**Controller:** `PublicEvidenceController`

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 150 | `GET` | `/api/v1/public/evidencia/{token}` | Ver evidencia de entrega por token (PDF inline) |

---

### 23.150 GET `/api/v1/public/evidencia/{token}`

Permite acceder a la evidencia de un entregable mediante un token seguro, sin autenticación.

**Response 200:** PDF (`application/pdf`, inline)
**Response 404:** Token inválido o evidencia no encontrada

---

## 24. Módulo Dashboard

**Base path:** `/api/v1/dashboard`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `DashboardController` | **Creado:** 2026-04-23

| # | Método | Endpoint | Descripción |
| --- | ------ | -------- | ----------- |
| 83 | `GET` | `/api/v1/dashboard/kpis` | KPIs globales del dashboard |
| 84 | `GET` | `/api/v1/dashboard/avance-por-proyecto` | Avance por proyecto |
| 85 | `GET` | `/api/v1/dashboard/proyectos/{id}/summary` | Resumen de un proyecto específico |
| 86 | `GET` | `/api/v1/dashboard/proyectos-por-dependencia` | Proyectos agrupados por dependencia |

---

## 25. Módulo Proyectos

**Base path:** `/api/v1/proyectos`
**Auth:** `@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")`
**Controller:** `ProyectoController` | **Creado:** 2026-04-23

| # | Método | Endpoint | Descripción | Permiso |
| --- | ------ | -------- | ----------- | ------- |
| 3 | `GET` | `/api/v1/proyectos` | Listar proyectos (con filtros y paginación) | `PROYECTO:VER` |
| 4 | `GET` | `/api/v1/proyectos/mis-proyectos` | Listar proyectos asignados al usuario | `PROYECTO:VER` (propios) |
| 5 | `GET` | `/api/v1/proyectos/directores-asignables` | Listar directores disponibles | `PROYECTO:CREAR` |
| 6 | `GET` | `/api/v1/proyectos/{id}` | Obtener proyecto por ID | `PROYECTO:VER` |
| 7 | `POST` | `/api/v1/proyectos` | Crear proyecto (**DESHABILITADO**) | `PROYECTO:CREAR` |
| 8 | `GET` | `/api/v1/proyectos/siguiente-codigo` | Obtener siguiente código secuencial | `PROYECTO:CREAR` |
| 9 | `POST` | `/api/v1/proyectos/registro-inicial` | Registro inicial de proyecto | `PROYECTO:CREAR` |
| 10 | `GET` | `/api/v1/proyectos/{id}/completion-status` | Estado de completitud del proyecto | `PROYECTO:VER` |
| 11 | `PUT` | `/api/v1/proyectos/{id}/completar-informacion` | Completar información inicial | `canCompleteInitialRegistration` |
| 12 | `PUT` | `/api/v1/proyectos/{id}` | Actualizar proyecto | `PROYECTO:EDITAR` |
| 13 | `GET` | `/api/v1/proyectos/dashboard` | Métricas del dashboard de proyectos | `PROYECTO:VER` |
| 14 | `DELETE` | `/api/v1/proyectos/{id}` | Eliminar proyecto (soft delete) | `PROYECTO:EDITAR` |
| 15 | `GET` | `/api/v1/proyectos/{id}/resumen` | Resumen del proyecto | `PROYECTO:VER` |
| 16 | `PATCH` | `/api/v1/proyectos/{id}/cerrar` | Cerrar proyecto | `PROYECTO:CERRAR` |
| 17 | `GET` | `/api/v1/proyectos/{id}/furag` | Obtener FURAG del proyecto | `PROYECTO:VER` |
| 18 | `PUT` | `/api/v1/proyectos/{id}/furag` | Actualizar FURAG | `PROYECTO:EDITAR` |
| 19 | `POST` | `/api/v1/proyectos/recalcular-avances` | Recalcular avances (admin) | `SISTEMA:CONFIGURAR` |

---

### 25.9 POST `/api/v1/proyectos/registro-inicial`

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

### 25.3 GET `/api/v1/proyectos`

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
| `sort` | string | No | Ordenamiento (default: `id`) |

**Response 200:** `Page<ProyectoListDTO>`

---

## Códigos de Error

| Código HTTP | Significado                          | Acción recomendada                         |
| ----------- | ------------------------------------ | ------------------------------------------ |
| `200`       | Operación exitosa                    | —                                          |
| `201`       | Recurso creado exitosamente          | —                                          |
| `204`       | Eliminación exitosa (sin contenido)  | —                                          |
| `400`       | Solicitud inválida                   | Revisar parámetros y body                  |
| `401`       | No autenticado                       | Verificar token JWT                        |
| `403`       | Sin permisos                         | Verificar permisos/rol en Keycloak         |
| `404`       | Recurso no encontrado                | Verificar ID/ruta                          |
| `409`       | Conflicto (duplicado/concurrente)    | Reintentar o verificar estado              |
| `413`       | Archivo demasiado grande             | Reducir tamaño del archivo                 |
| `415`       | Tipo de archivo no soportado         | Verificar formato aceptado                 |
| `422`       | Entidad no procesable                | Verificar formato de datos                 |
| `429`       | Límite de solicitudes excedido       | Esperar y reintentar con backoff           |
| `500`       | Error interno del servidor           | Contactar equipo de soporte                |

### Formato de Error Estándar

```json
{
  "timestamp": "2026-07-29T15:45:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Mensaje descriptivo del error",
  "path": "/api/v1/proyectos/{id}/riesgos",
  "details": [
    { "field": "nombre", "message": "El nombre es obligatorio" }
  ]
}
```

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

*Documento generado el 2026-07-29 con los 150 endpoints reales del sistema, ordenados del más reciente al más antiguo por fecha de incorporación.*
