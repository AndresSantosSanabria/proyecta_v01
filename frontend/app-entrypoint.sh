#!/bin/sh
set -e

CONFIG_FILE="/usr/share/nginx/html/config.js"

# Genera config.js en runtime a partir de las variables de entorno del contenedor.
# Asi la misma imagen sirve cualquier entorno (dev, QA, prod) sin reconstruir.
# Claves planas (sin prefijo VITE_): env.js las busca como KEYCLOAK_BASE_URL, API_URL, etc.
cat > "$CONFIG_FILE" <<EOF
window.__APP_CONFIG__ = {
  API_URL: "${VITE_API_URL:-}",
  ENABLE_AUTH: "${VITE_ENABLE_AUTH:-true}",
  KEYCLOAK_BASE_URL: "${VITE_KEYCLOAK_BASE_URL:-}",
  KEYCLOAK_REALM: "${VITE_KEYCLOAK_REALM:-}",
  KEYCLOAK_CLIENT_ID: "${VITE_KEYCLOAK_CLIENT_ID:-}",
  KEYCLOAK_REDIRECT_URI: "${VITE_KEYCLOAK_REDIRECT_URI:-}",
  KEYCLOAK_POST_LOGOUT_REDIRECT_URI: "${VITE_KEYCLOAK_POST_LOGOUT_REDIRECT_URI:-}",
  KEYCLOAK_RESPONSE_TYPE: "${VITE_KEYCLOAK_RESPONSE_TYPE:-code}",
  KEYCLOAK_SCOPE: "${VITE_KEYCLOAK_SCOPE:-openid profile email}"
};
EOF

exec "$@"
