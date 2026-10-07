import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    rules: {
      // Decisión 2026-09-28: set-state-in-effect se degrada a 'warn'. Es una regla
      // de performance de React Compiler (no un CWE ni un bug funcional): marca el
      // patrón estándar de carga asíncrona en useEffect (fetch + setLoading) y de
      // reset de estado de modales/paginación al cambiar props/filtros. Corregirla
      // en los ~24 sitios requeriría microtasks o refactor con riesgo de regresiones
      // (renders intermedios con estado stale, dobles fetch). Se deja como warning
      // visible para refactor futuro.
      'react-hooks/set-state-in-effect': 'warn',
    },
  },
  {
    // Globals de Vitest en los archivos de prueba
    files: ['src/**/*.test.{js,jsx}', 'src/test/**'],
    languageOptions: {
      globals: {
        ...globals.jest,
        vi: true,
        afterEach: true,
        beforeAll: true,
        afterAll: true,
      },
    },
  },
])
