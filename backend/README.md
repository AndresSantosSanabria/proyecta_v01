# api-gestion — Proyecta

Sistema de gestión de proyectos y dashboard analítico desarrollado con Spring Boot.

---

## Tabla de Contenidos

- [Descripción](#descripción)
- [Tecnologías](#tecnologías)
- [Requisitos Previos](#requisitos-previos)
- [Instalación](#instalación)
- [Configuración](#configuración)
- [Ejecución](#ejecución)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Endpoints API](#endpoints-api)
- [Módulo: Ciclo de Vida Documental](#módulo-ciclo-de-vida-documental)
- [Frontend React](#frontend-react)
- [Pruebas](#pruebas)
- [Despliegue](#despliegue)
- [Troubleshooting](#troubleshooting)
- [Contribuir](#contribuir)
- [Licencia](#licencia)
- [Contacto](#contacto)

---

## Descripción

**api-gestion** es el backend REST del sistema **Proyecta**, diseñado para la gestión de proyectos y un dashboard analítico con trazabilidad completa según estándares ITIL. El sistema incluye soporte para el seguimiento del ciclo de vida documental.

---

## Tecnologías

| Componente       | Tecnología                             | Versión     |
| ---------------- | -------------------------------------- | ----------- |
| Lenguaje         | Java                                   | 25          |
| Framework        | Spring Boot                            | 4.0.5       |
| Persistencia     | Spring Data JPA + Hibernate            | 6.x         |
| Base de datos    | PostgreSQL                             | 16+         |
| Documentación    | Swagger / OpenAPI 3                    | —           |
| Seguridad        | OAuth2 Resource Server + Keycloak      | 24+         |
| Build            | Maven                                  | 3.9+        |
| Frontend         | React + oidc-client-ts                 | 18+         |

---

## Requisitos Previos

- **Java JDK 25** (o superior compatible con Spring Boot 4.x)
- **Maven 3.9+**
- **PostgreSQL 16+**
- **Node.js 18+** y **npm 9+** (solo para frontend)
- **Keycloak 24+** (servidor de identidad)
- **Git**

---

## Instalación

### 1. Clonar el repositorio

```bash
git clone https://github.com/USUARIO/api-gestion.git
cd api-gestion
```

### 2. Instalar dependencias

```bash
./mvnw clean install
```

O si Maven está instalado globalmente:

```bash
mvn clean install
```

### 3. Configurar base de datos

Crear la base de datos en PostgreSQL:

```sql
CREATE DATABASE proyecta;
CREATE USER proyecta_user WITH PASSWORD 'tu_contraseña';
GRANT ALL PRIVILEGES ON DATABASE proyecta TO proyecta_user;
```

### 4. Configurar variables

Copia y ajusta las variables de entorno (ver [Configuración](#configuración)).

---

## Configuración

### Archivo principal

La configuración se encuentra en:

```
src/main/resources/application.properties
```

### Variables de Entorno

| Variable                              | Descripción                         | Ejemplo                                   |
| ------------------------------------- | ----------------------------------- | ----------------------------------------- |
| `SPRING_DATASOURCE_URL`              | URL de conexión a PostgreSQL        | `jdbc:postgresql://localhost:5432/proyecta` |
| `SPRING_DATASOURCE_USERNAME`         | Usuario de base de datos            | `proyecta_user`                           |
| `SPRING_DATASOURCE_PASSWORD`         | Contraseña de base de datos         | `***`                                     |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | Issuer de Keycloak | Ver Keycloak |
| `GOB_SECURITY_CORS_ALLOWED-ORIGINS`  | Orígenes permitidos CORS            | `http://localhost:5173`                    |

### Keycloak

| Propiedad   | Valor                                                    |
| ----------- | -------------------------------------------------------- |
| Realm       | `gob-cundinamarca-devqa`                                 |
| Issuer      | `http://172.20.6.59:8080/realms/gob-cundinamarca-devqa`  |
| Client ID   | `proyecta-web`                                           |
| Rol requerido | `app_access`                                           |

### Perfiles de Spring

```bash
# Desarrollo
java -jar target/api-gestion-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# Producción
java -jar target/api-gestion-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## Ejecución

### Backend

```bash
# Usando Maven Wrapper (recomendado)
./mvnw spring-boot:run

# O con Maven global
mvn spring-boot:run

# O ejecutar el JAR generado
java -jar target/api-gestion-0.0.1-SNAPSHOT.jar
```

El servidor arranca en: `http://localhost:8080`

### Swagger / OpenAPI

Una vez ejecutado el backend, la documentación interactiva está disponible en:

```
http://localhost:8080/swagger-ui.html
http://localhost:8080/v3/api-docs
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

El frontend arranca en: `http://localhost:5173`

---

## Estructura del Proyecto

```
api-gestion/
├── docs/
│   ├── arquitectura-tecnica.md    # Documento de Arquitectura Técnica
│   └── api-endpoints.md           # Documentación de Endpoints API
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/proyecta/
│   │   │       ├── controller/    # Controladores REST
│   │   │       ├── service/       # Lógica de negocio
│   │   │       ├── repository/    # Acceso a datos (JPA)
│   │   │       ├── model/         # Entidades de dominio
│   │   │       ├── dto/           # Data Transfer Objects
│   │   │       ├── config/        # Configuración (Security, CORS, etc.)
│   │   │       └── exception/     # Manejo de excepciones
│   │   └── resources/
│   │       └── application.properties
│   └── test/                      # Pruebas unitarias e de integración
├── CHANGELOG.md                   # Historial de cambios (Keep a Changelog)
├── README.md                      # Este archivo
├── pom.xml                        # Descriptor Maven
└── mvnw / mvnw.cmd               # Maven Wrapper
```

---

## Endpoints API

Los endpoints están documentados detalladamente en [`docs/api-endpoints.md`](docs/api-endpoints.md).

### Resumen

| Método  | Endpoint                                  | Descripción                    |
| ------- | ----------------------------------------- | ------------------------------ |
| `POST`  | `/api/v1/documents`                       | Crear documento                |
| `GET`   | `/api/v1/documents/{id}`                  | Obtener documento por ID       |
| `GET`   | `/api/v1/documents`                       | Listar documentos (filtros)    |
| `PATCH` | `/api/v1/documents/{id}/status`           | Transicionar estado            |
| `GET`   | `/api/v1/documents/{id}/history`          | Historial de estados           |
| `DELETE`| `/api/v1/documents/{id}`                  | Soft delete                    |

---

## Módulo: Ciclo de Vida Documental

El módulo de **Seguimiento del Ciclo de Vida Documental** gestiona el flujo completo de estados de un documento:

```
BORRADOR → EN_REVISION → APROBADO → PUBLICADO → VIGENTE → OBSOLETO → ARCHIVADO
                ↓                                           ↓
            RECHAZADO                                   RETIRADO
```

La documentación completa del módulo se encuentra en:

- **Arquitectura Técnica**: [`docs/arquitectura-tecnica.md`](docs/arquitectura-tecnica.md)
- **Endpoints API**: [`docs/api-endpoints.md`](docs/api-endpoints.md)
- **Historial de Cambios**: [`CHANGELOG.md`](CHANGELOG.md)

---

## Frontend React

El frontend debe usar **OIDC con PKCE**. No construir la URL de login manualmente.

```bash
npm install oidc-client-ts
```

```js
import { UserManager } from "oidc-client-ts";

export const auth = new UserManager({
  authority: "http://172.20.6.59:8080/realms/gob-cundinamarca-devqa",
  client_id: "proyecta-web",
  redirect_uri: "http://localhost:5173/callback",
  post_logout_redirect_uri: "http://localhost:5173/",
  response_type: "code",
  scope: "openid profile email",
  automaticSilentRenew: true,
});
```

**Flujo de autenticación:**

1. React ejecuta `auth.signinRedirect()`
2. Keycloak autentica al usuario
3. React procesa el callback con `auth.signinRedirectCallback()`
4. React llama a Spring con `Authorization: Bearer <token>`
5. Spring valida el JWT y aplica `hasRole('app_access')`

**Flujo de logout recomendado:**

1. React limpia el `access_token`, `refresh_token` e `id_token` almacenados localmente.
2. React llama a `POST /api/v1/authz/logout` enviando `idToken` y, si existe, `refreshToken`.
3. La API invalida la sesión local, revoca el refresh token cuando aplica y devuelve la `logoutUrl`.
4. React redirige el navegador a `logoutUrl` para cerrar la sesión SSO en Keycloak.
5. Si una llamada a la API responde `401`, el frontend debe intentar renovar con el refresh token; si la renovación falla, debe reenviar al usuario al login.

```js
// Ejemplo de manejo de 401/403 en el cliente
if (response.status === 401) {
  const renewed = await auth.signinSilent().catch(() => null);
  if (!renewed) {
    await auth.signinRedirect();
  }
}

if (response.status === 403) {
  // El token sigue siendo válido, pero el usuario no tiene permisos
  showForbiddenMessage();
}
```

---

## Pruebas

```bash
# Ejecutar todas las pruebas
./mvnw test

# Ejecutar pruebas específicas
./mvnw test -Dtest=DocumentLifecycleServiceTest

# Cobertura de código
./mvnw jacoco:report
```

Los reportes de cobertura se generan en `target/site/jacoco/index.html`.

---

## Despliegue

### Docker (Recomendado)

```bash
docker-compose up -d
```

### JAR standalone

```bash
./mvnw clean package -DskipTests
java -jar target/api-gestion-0.0.1-SNAPSHOT.jar
```

### Variables de entorno para producción

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/proyecta
export SPRING_DATASOURCE_USERNAME=proyecta_user
export SPRING_DATASOURCE_PASSWORD=contraseña_segura
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://keycloak.dominio.com/realms/gob-cundinamarca
export GOB_SECURITY_CORS_ALLOWED-ORIGINS=https://frontend.dominio.com
```

---

## Troubleshooting

### PostgreSQL de Laragon no arranca

Si PostgreSQL de Laragon no arranca por un `postmaster.pid` huérfano:

```bash
start-postgres-laragon.cmd
```

Ejecutar desde la raíz del proyecto. El script limpia el bloqueo y levanta el cluster en `127.0.0.1:5432`.

### CORS bloqueado

En desarrollo, el backend permite `http://localhost:5173`. Si el frontend usa otro origen, ajustar `gob.security.cors.allowed-origins` en `application.properties`.

### Error 403 en endpoints

Verificar que el JWT contenga el rol `app_access` en el claim `realm_access.roles`. Revisar la configuración del client en Keycloak.

---

## Contribuir

1. Crear una rama feature: `git checkout -b feature/nombre-funcionalidad`
2. Hacer commit siguiendo el formato del [CHANGELOG](CHANGELOG.md)
3. Push a la rama: `git push origin feature/nombre-funcionalidad`
4. Crear un Pull Request

### Convenciones de Commits

| Prefijo     | Uso                                      |
| ----------- | ---------------------------------------- |
| `feat:`     | Nueva funcionalidad                      |
| `fix:`      | Corrección de bug                        |
| `docs:`     | Cambios en documentación                 |
| `refactor:` | Refactorización sin cambio funcional     |
| `test:`     | Añadir o modificar pruebas               |
| `chore:`    | Tareas de mantenimiento                  |

---

## Licencia

_[Especificar licencia: MIT, Apache 2.0, etc.]_

---

## Contacto

| Rol                  | Nombre            | Email                        |
| -------------------- | ----------------- | ---------------------------- |
| Arquitecto de Software | _[Nombre]_      | _[email]_                    |
| Tech Lead            | _[Nombre]_        | _[email]_                    |
| Equipo de Soporte    | —                 | _[email de soporte]_         |

---

*Última actualización: 2026-07-29*
