# api-gestion

Sistema de gestion de proyectos y dashboard analitico desarrollado con Spring Boot.

## Tecnologias

- Lenguaje: Java 25
- Framework: Spring Boot 4.0.5
- Persistencia: Spring Data JPA + Hibernate
- Base de datos: PostgreSQL
- Documentacion: Swagger / OpenAPI 3
- Seguridad: OAuth2 Resource Server con Keycloak

## Configuracion

1. Copia `src/main/resources/application.properties.template` a `src/main/resources/application.properties`.
2. Ajusta la conexion a base de datos.
3. Verifica las variables de Keycloak:
   - `KEYCLOAK_ISSUER_URI`
   - `KEYCLOAK_JWKS_URI`
   - `GOB_RESOURCE_CLIENT_IDS`
   - `GOB_CORS_ORIGINS`

## Keycloak

- Realm: `gob-cundinamarca-devqa`
- Issuer: `http://172.20.6.59:8080/realms/gob-cundinamarca-devqa`
- Client ID: `proyecta-web`
- Rol requerido para entrar al sistema: `app_access`

## Frontend React

El frontend debe usar OIDC con PKCE. No construyas la URL de login a mano.

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

Flujo:

1. React ejecuta `auth.signinRedirect()`.
2. Keycloak autentica al usuario.
3. React procesa el callback con `auth.signinRedirectCallback()`.
4. React llama a Spring con `Authorization: Bearer <token>`.
5. Spring valida el JWT y aplica `hasRole('app_access')`.

## CORS

En desarrollo, el backend permite `http://localhost:5173`. Si el frontend usa otro origen, ajusta `gob.security.cors.allowed-origins`.

