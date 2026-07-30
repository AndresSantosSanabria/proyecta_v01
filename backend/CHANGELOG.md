# Changelog

Todos los cambios notables en el módulo **Seguimiento del Ciclo de Vida Documental** se documentan en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/),
y el proyecto adhiere al [Semantic Versioning](https://semver.org/lang/es/).

---

## [Unreleased]

### Added

- _[Funcionalidad nueva pendiente de release]_

### Changed

- _[Cambio en funcionalidad existente pendiente de release]_

### Deprecated

- _[Funcionalidad que será removida en futuras versiones]_

### Removed

- _[Funcionalidad removida pendiente de release]_

### Fixed

- _[Bug corregido pendiente de release]_

### Security

- _[Parche de seguridad pendiente de release]_

---

## [1.0.0] - 2026-07-29

### Added

- Creación del módulo Seguimiento del Ciclo de Vida Documental
- Modelo de dominio: `Document`, `DocumentStatus`, `AuditLog`
- Estados del ciclo de vida: BORRADOR, EN_REVISION, RECHAZADO, APROBADO, PUBLICADO, VIGENTE, OBSOLETO, RETIRADO, ARCHIVADO, ELIMINADO
- Reglas de transición de estados con validación en `DocumentLifecycleService`
- Endpoints REST:
  - `POST /api/v1/documents` — Crear documento
  - `GET /api/v1/documents/{id}` — Obtener documento por ID
  - `GET /api/v1/documents` — Listar documentos con filtros y paginación
  - `PATCH /api/v1/documents/{id}/status` — Transicionar estado
  - `GET /api/v1/documents/{id}/history` — Historial de estados
  - `DELETE /api/v1/documents/{id}` — Soft delete
- Auditoría completa con tabla `AUDIT_LOG` (acción, valores anterior/nuevo, usuario, timestamp, metadata)
- Notificaciones asíncronas por email al cambiar de estado
- Autenticación OAuth2 Resource Server con Keycloak
- Autorización por rol `app_access`
- Soft delete con campo `deleted_at`
- Documentación de arquitectura técnica (`docs/arquitectura-tecnica.md`)
- Documentación de endpoints API (`docs/api-endpoints.md`)

### Security

- JWT Bearer token validation con Keycloak
- CORS configurado para orígenes permitidos
- Rate limiting en endpoints de escritura (100 req/min/usuario)

---

## Guía de Categorías

Cada entrada en el changelog debe clasificarse en una de las siguientes categorías según [Keep a Changelog](https://keepachangelog.com/es/1.1.0/):

| Categoría     | Descripción                                                                 |
| ------------- | --------------------------------------------------------------------------- |
| **Added**     | Funcionalidades nuevas                                                      |
| **Changed**   | Modificaciones en funcionalidades existentes que rompen compatibilidad      |
| **Deprecated**| Funcionalidades que serán eliminadas en versiones futuras                   |
| **Removed**   | Funcionalidades eliminadas                                                  |
| **Fixed**     | Corrección de bugs                                                          |
| **Security**  | Parches de vulnerabilidades de seguridad                                    |

---

## Convenciones de Versionado

El proyecto sigue [Semantic Versioning](https://semver.org/lang/es/):

| Tipo de cambio         | Incremento  | Ejemplo           |
| ---------------------- | ----------- | ----------------- |
| Breaking change (API)  | Mayor       | 1.0.0 → 2.0.0     |
| Nueva funcionalidad    | Menor       | 1.0.0 → 1.1.0     |
| Bug fix / parche       | Patch       | 1.0.0 → 1.0.1     |

### Formato de Enlace para GitHub

```markdown
[Unreleased]: https://github.com/USUARIO/REPOSITORIO/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/USUARIO/REPOSITORIO/releases/tag/v1.0.0
```

---

## Plantilla para Nuevas Entradas

Copiar y adaptar para cada release:

```markdown
## [X.Y.Z] - AAAA-MM-DD

### Added

- Descripción de funcionalidad añadida

### Changed

- Descripción de cambio en funcionalidad existente

### Deprecated

- Descripción de funcionalidad deprecada

### Removed

- Descripción de funcionalidad eliminada

### Fixed

- Descripción de bug corregido

### Security

- Descripción de parche de seguridad
```
