# ---------- Build ----------
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .

# Variables de entorno de Vite (build-time)
ARG VITE_API_URL=http://localhost:8082
ARG VITE_BASE_PATH=/
ARG VITE_ENABLE_AUTH=true
ARG VITE_KEYCLOAK_BASE_URL=
ARG VITE_KEYCLOAK_REALM=
ARG VITE_KEYCLOAK_CLIENT_ID=
ARG VITE_KEYCLOAK_REDIRECT_URI=http://localhost:8080/callback
ARG VITE_KEYCLOAK_POST_LOGOUT_REDIRECT_URI=http://localhost:8080/
ARG VITE_KEYCLOAK_RESPONSE_TYPE=code
ARG VITE_KEYCLOAK_SCOPE="openid profile email"

ENV VITE_API_URL=$VITE_API_URL \
    VITE_BASE_PATH=$VITE_BASE_PATH \
    VITE_ENABLE_AUTH=$VITE_ENABLE_AUTH \
    VITE_KEYCLOAK_BASE_URL=$VITE_KEYCLOAK_BASE_URL \
    VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM \
    VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID \
    VITE_KEYCLOAK_REDIRECT_URI=$VITE_KEYCLOAK_REDIRECT_URI \
    VITE_KEYCLOAK_POST_LOGOUT_REDIRECT_URI=$VITE_KEYCLOAK_POST_LOGOUT_REDIRECT_URI \
    VITE_KEYCLOAK_RESPONSE_TYPE=$VITE_KEYCLOAK_RESPONSE_TYPE \
    VITE_KEYCLOAK_SCOPE=$VITE_KEYCLOAK_SCOPE

RUN npm run build

# ---------- Runtime ----------
# Imagen de nginx sin privilegios (corre como usuario no-root, puerto 8080)
FROM nginxinc/nginx-unprivileged:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
