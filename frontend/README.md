# Proyecta - Sistema de Gestión de Proyectos

Frontend moderno desarrollado con **React** y **Vite** para la gestión integral de proyectos, cronogramas, progreso y documentación.

##  Características Principales

- **Gestión de Proyectos** - Crear, editar y visualizar proyectos
-  **Dashboard** - Panel de control con métricas e información general
-  **Cronograma (Gantt)** - Visualizar timeline de proyectos con fases e hitos
-  **Seguimiento de Progreso** - Monitorear KPIs y avance del proyecto
-  **Gestión Documental** - Carga y administración de documentos
-  **Evidencias** - Subida de evidencias del proyecto
-  **Asistente (Wizard)** - Creación paso a paso de nuevos proyectos
-  **Reportes** - Generación de reportes del proyecto
-  **Tema Personalizable** - Soporte para modo claro/oscuro

##  Requisitos Previos

- **Node.js** >= 16.x
- **npm** >= 8.x

##  Instalación

1. **Clonar el repositorio:**
```bash
git clone <URL_DEL_REPOSITORIO>
cd Proyecta-Frontend
```

2. **Instalar dependencias:**
```bash
npm install
```

3. **Configurar variables de entorno:**
```bash
cp .env.template .env
```

Editar `.env` y configurar los valores según tu entorno:
```env
VITE_API_URL=http://localhost:8080/api/v1
VITE_ENABLE_AUTH=false
```

##  Scripts Disponibles

```bash
# Desarrollo con Hot Module Replacement (HMR)
npm run dev

# Compilar para producción
npm run build

# Validar código con ESLint
npm run lint

# Vista previa del build de producción
npm run preview
```

##  Estructura del Proyecto

```
src/
├── api/                      # Configuración de Axios
├── assets/                   # Imágenes y recursos estáticos
├── components/
│   ├── common/              # Componentes reutilizables
│   │   ├── ErrorBoundary/
│   │   ├── DocumentUpload/
│   │   ├── EvidenceUpload/
│   │   └── KPICard/
│   ├── features/            # Componentes específicos de features
│   │   ├── cronograma/      # Gestión de cronogramas y Gantt
│   │   ├── progress/        # Seguimiento de progreso
│   │   ├── projects/        # Listado y tablas de proyectos
│   │   └── wizard/          # Asistente paso a paso
│   └── layout/              # Componentes de estructura
│       └── SidebarLayout/
├── constants/               # Constantes de la aplicación
├── context/                 # React Context (Tema, etc)
├── hooks/                   # Custom hooks
├── pages/                   # Páginas principales
│   ├── DashboardPage/
│   ├── ProjectsPage/
│   ├── NewProjectPage/
│   ├── ProjectProgressPage/
│   ├── ProjectClosurePage/
│   ├── CronogramaPage/
│   └── ReportsPage/
├── services/                # Servicios de API
├── utils/                   # Funciones de utilidad
├── App.jsx                  # Componente raíz
├── main.jsx                 # Punto de entrada
└── index.css                # Estilos globales
```

##  Dependencias Principales

- **react** ^19.2.6 - Librería UI
- **react-router-dom** ^7.15.0 - Enrutamiento
- **axios** ^1.16.0 - Cliente HTTP
- **@tanstack/react-query** ^5.100.10 - Gestión de estado y caché de datos
- **lucide-react** ^1.14.0 - Iconografía
- **prop-types** ^15.8.1 - Validación de props

##  Flujo de Desarrollo

### Creación de un Nuevo Proyecto
1. Navegar a "Nuevo Proyecto"
2. Seguir el asistente paso a paso:
   - Paso 1: Datos generales
   - Paso 2: Patrocinador y equipo
   - Paso 3: Fases, hitos y entregables
   - Paso 4: PETI y comunicaciones
   - Paso 5: FURAG
   - Paso 6: Gestión documental

### Gestión de Cronogramas
- Ver cronograma en formato Gantt
- Visualizar fases y hitos
- Subir cronogramas actualizados

### Seguimiento de Progreso
- Ver KPIs del proyecto
- Árbol de tareas/entregables
- Actualizar estado de progreso

### Gestión de Documentación
- Subir documentos del proyecto
- Subir evidencias
- Descargar archivos

##  Variables de Entorno

```env
# API
VITE_API_URL              # URL base del backend API (por defecto: http://localhost:8080/api/v1)

# Seguridad
VITE_ENABLE_AUTH          # Habilitar autenticación (true/false, por defecto: false)

# Aplicación
VITE_APP_TITLE            # Título de la aplicación
VITE_APP_MODE             # Modo (development/production)
VITE_LOG_LEVEL            # Nivel de logs (debug/info/warning/error)
```

##  Conexión con Backend

El frontend se conecta con un backend API REST en:
```
http://localhost:8080/api/v1
```

Endpoints esperados:
- `GET /projects` - Listar proyectos
- `POST /projects` - Crear proyecto
- `GET /projects/:id` - Obtener detalle del proyecto
- `PUT /projects/:id` - Actualizar proyecto
- `DELETE /projects/:id` - Eliminar proyecto
- `POST /documents/upload` - Subir documentos
- `POST /evidence/upload` - Subir evidencias

##  Customización

### Cambiar Tema
El proyecto incluye soporte para tema claro/oscuro mediante `ThemeContext`.

### Agregar Nuevas Páginas
1. Crear carpeta en `src/pages/MiPaginaNueva/`
2. Crear componente `MiPaginaNueva.jsx`
3. Crear estilos `MiPaginaNueva.css`
4. Importar y configurar ruta en App.jsx

### Agregar Nuevos Servicios
1. Crear archivo en `src/services/miServicio.js`
2. Exportar funciones de API
3. Usar en componentes con `useQuery` o `useMutation`

##  Troubleshooting

### Puerto 5173 en uso
```bash
npm run dev -- --port 3000
```

### Problemas de CORS
Asegurar que el backend permita requests desde `http://localhost:5173`

### node_modules corrupto
```bash
rm -r node_modules package-lock.json
npm install
```




**Última actualización:** Mayo 2026
