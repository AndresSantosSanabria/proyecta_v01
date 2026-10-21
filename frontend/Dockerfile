# ---------- Build ----------
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .

# Solo VITE_BASE_PATH se hornea en el build (define el prefijo de assets de Vite).
# El resto de la configuracion (API, Keycloak, URLs) se inyecta en runtime
# via /config.js generado por docker-entrypoint.sh — la misma imagen sirve
# cualquier entorno sin reconstruir.
ARG VITE_BASE_PATH=/
ENV VITE_BASE_PATH=$VITE_BASE_PATH

RUN npm run build

# ---------- Runtime ----------
# Imagen de nginx sin privilegios (corre como usuario nginx, puerto 8080)
FROM nginxinc/nginx-unprivileged:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
# --chmod requiere BuildKit (habilitado por defecto en Docker Desktop / compose v2)
COPY --chmod=755 app-entrypoint.sh /app-entrypoint.sh
# El html debe ser escribible por nginx para generar /config.js en cada arranque
USER root
RUN chown -R nginx:nginx /usr/share/nginx/html
USER nginx
EXPOSE 8080
ENTRYPOINT ["/app-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
