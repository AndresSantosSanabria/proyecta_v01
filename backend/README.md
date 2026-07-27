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

1. Ajusta la conexion a base de datos directamente en `src/main/resources/application.properties`.
2. Verifica las propiedades de Keycloak, correo y CORS en ese mismo archivo.
3. Si prefieres usar un perfil manual, copia `src/main/resources/application.properties.template` como referencia.
4. La contraseña de PostgreSQL se define en `spring.datasource.password` dentro de ese archivo.

## Laragon / PostgreSQL

Si PostgreSQL de Laragon no arranca por un `postmaster.pid` huerfano, ejecuta
`start-postgres-laragon.cmd` desde la raiz del proyecto. El script limpia el
bloqueo y levanta el cluster de Laragon en `127.0.0.1:5432`.

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


