# Implementación OAuth2 OIDC - Keycloak

Esta guía detalla la integración completa de Keycloak con Spring Boot y React.

---

##  Requisitos Previos

- **Keycloak**: Accesible en `http://172.20.6.59:8080`
  - Realm: `gob-cundinamarca-devqa`
  - Client ID: `proyecta-web`
  - Client authentication: OFF
  - Flow: Standard flow (PKCE: S256)

- **React**: Ya tiene `react-oidc-context` y `oidc-client-ts` instalados

- **Spring Boot**: Java 25, Spring Boot 4.0.5

---

## Implementación Rápida

### Variables de entorno recomendadas

Usa estas variables en `.env` para evitar hardcodear Keycloak:

```env
VITE_KEYCLOAK_BASE_URL=http://172.20.6.59:8080
VITE_KEYCLOAK_REALM=gob-cundinamarca-devqa
VITE_KEYCLOAK_CLIENT_ID=proyecta-web
VITE_KEYCLOAK_REDIRECT_URI=http://localhost:5173/callback
VITE_KEYCLOAK_POST_LOGOUT_REDIRECT_URI=http://localhost:5173/
```

Si el entorno de infraestructura cambia, ajusta solo estas variables.

###  Backend Spring Boot

#### Compilar y ejecutar:
```bash
cd D:\SpringBoot\PROYECTA
mvn clean install

# En otra terminal
cd api-gestion
mvn spring-boot:run
```

El backend estará en: `http://localhost:8081`

#### Archivos configurados:
- [application.properties](D:\SpringBoot\PROYECTA\api-gestion\src\main\resources\application.properties)
-  [SecurityConfig.java](D:\SpringBoot\PROYECTA\api-gestion\src\main\java\com\proyecta\api_gestion\config\SecurityConfig.java)
- [KeycloakRealmRoleConverter.java](D:\SpringBoot\PROYECTA\api-gestion\src\main\java\com\proyecta\api_gestion\security\KeycloakRealmRoleConverter.java)
- [WelcomeController.java](D:\SpringBoot\PROYECTA\api-gestion\src\main\java\com\proyecta\api_gestion\controller\WelcomeController.java)

---

###  React Frontend

#### Archivos creados:

1. **Configuración OIDC** - [src/config/oidcConfig.js](src/config/oidcConfig.js)
   ```javascript
   import oidcConfig from './config/oidcConfig.js'
   // Contiene: authority, client_id, redirect_uri
   ```

2. **AuthContext** - [src/context/AuthContext.jsx](src/context/AuthContext.jsx)
   ```javascript
   import { useAuthContext } from './context/AuthContext'
   const { user, accessToken, isAuthenticated, login, logout } = useAuthContext()
   ```

3. **Hook para API con Token** - [src/hooks/useApiWithAuth.js](src/hooks/useApiWithAuth.js)
   ```javascript
   import { useApiWithAuth } from './hooks/useApiWithAuth'
   const { callApi } = useApiWithAuth()
   const data = await callApi('/api/app/bienvenida')
   ```

4. **Componente de Ejemplo** - [src/components/common/WelcomeSection.jsx](src/components/common/WelcomeSection.jsx)

#### Cambios en main.jsx:
-  Añadido `AuthProvider` de react-oidc-context
-  Añadido `AppAuthProvider` personalizado
-  Importado `oidcConfig`

#### Cambios en ProtectedRoute:
-  Usa `useAuth()` de react-oidc-context
-  Valida autenticación
-  Redirige a login si no autenticado

---

##  Pruebas

### Backend - Health check (sin autenticación):
```bash
curl http://localhost:8081/api/app/public/health
```

Respuesta esperada:
```json
{
  "status": "OK",
  "message": "Backend disponible"
}
```

### Backend - Endpoint protegido (con token):
```bash
curl -X GET http://localhost:8081/api/app/bienvenida \
  -H "Authorization: Bearer <access_token_de_keycloak>"
```

Respuesta esperada (200 OK):
```json
{
  "mensaje": "¡Bienvenido!",
  "usuario": "username",
  "roles": ["ROLE_APP_ACCESS"]
}
```

### Respuesta si no tiene rol:
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied"
}
```

---

##  Flujo Completo en React

### 1. Usuario intenta acceder:
```
http://localhost:5173 → ProtectedRoute detecta no autenticado
```

### 2. React redirige a Keycloak:
```
→ http://172.20.6.59:8080/auth/...?redirect_uri=http://localhost:5173/callback
```

### 3. Usuario inicia sesión en Keycloak

### 4. Keycloak redirige a callback:
```
→ http://localhost:5173/callback?code=...&state=...
```

### 5. React intercambia code por token (librería maneja)

### 6. React almacena token y hace requests:
```
GET /api/app/bienvenida
Authorization: Bearer eyJhbGc...
```

### 7. Backend valida y responde:
```
{
  "mensaje": "¡Bienvenido!",
  "usuario": "username",
  "roles": ["ROLE_APP_ACCESS"]
}
```

---

## Ejemplo de uso en un componente

```jsx
import { useApiWithAuth } from '../hooks/useApiWithAuth';
import { useAuthContext } from '../context/AuthContext';

function MiComponente() {
  const { user, logout } = useAuthContext();
  const { callApi, isAuthenticated } = useApiWithAuth();
  const [data, setData] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await callApi('/api/app/bienvenida');
        setData(response);
      } catch (error) {
        console.error('Error:', error);
      }
    };
    
    fetchData();
  }, [callApi]);

  return (
    <div>
      <h1>Hola {user?.profile?.name}</h1>
      {data && <p>{data.mensaje}</p>}
      <button onClick={logout}>Logout</button>
    </div>
  );
}
```

---

##  Troubleshooting

### "CORS error" o "No se puede conectar a 172.20.6.59:8080"
**Solución:** Verifica VPN/conexión a red interna de Gobernación

### "Access Denied" (403) en /api/app/bienvenida
**Solución:** El usuario no tiene el rol `app_access` en Keycloak. Asignarlo en:
- Keycloak → Clients → proyecta-web → Client Scopes → roles → Scope Mappings
- Luego asignar rol al usuario

### "Token inválido" o "Invalid signature"
**Solución:** Verificar que:
1. El issuer-uri en application.properties es correcto
2. Keycloak está accesible
3. El JWT aún no ha expirado

### React no persiste después de F5
**Solución:** Es normal con oidc-client-ts, se maneja automáticamente

---

## Dependencias

### React
```json
{
  "react-oidc-context": "^2.3.2",
  "oidc-client-ts": "^1.11.0"
}
```

### Spring Boot
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

---

##  Próximos Pasos

1. Compilar backend: `mvn clean install`
2. Ejecutar backend: `mvn spring-boot:run`
3. Ejecutar frontend: `npm run dev`
4. Probar flujo completo en http://localhost:5173
5. Verificar que el endpoint `/api/app/bienvenida` retorna datos

---
