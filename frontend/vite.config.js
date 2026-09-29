import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  return {
    // Prefijo de despliegue bajo subruta (ej. /apps/proyecta/public/).
    // Vacío o sin definir = raíz del dominio (dev local).
    base: env.VITE_BASE_PATH || '/',
    plugins: [react()],
  }
})
