// Placeholder de desarrollo local (npm run dev).
// En Docker este archivo es sobrescrito en runtime por docker-entrypoint.sh
// con las variables de entorno del contenedor.
// Si __APP_CONFIG__ no aporta valores, env.js cae a import.meta.env (.env local).
window.__APP_CONFIG__ = window.__APP_CONFIG__ || {};
