import { Routes, Route, Navigate, Outlet } from 'react-router-dom';
import SidebarLayout from './components/layout/SidebarLayout/SidebarLayout';
import ProtectedRoute from './components/ProtectedRoute/ProtectedRoute';
import PermissionRoute from './components/ProtectedRoute/PermissionRoute';
import AdminRoute from './components/ProtectedRoute/AdminRoute';
import AnalyticsRoute from './components/ProtectedRoute/AnalyticsRoute';
import ProjectAccessRoute from './components/ProtectedRoute/ProjectAccessRoute';
import { useAuthContext } from './context/AuthContext';
import DashboardPage from './pages/DashboardPage/DashboardPage';
import ProjectsPage from './pages/ProjectsPage/ProjectsPage';
import ReportsPage from './pages/ReportsPage/ReportsPage';
import AnalyticsPage from './pages/AnalyticsPage/AnalyticsPage';
import NewProjectPage from './pages/NewProjectPage/NewProjectPage';
import ProjectProgressPage from './pages/ProjectProgressPage/ProjectProgressPage';
import CronogramaPage from './pages/CronogramaPage/CronogramaPage';
import ProjectClosurePage from './pages/ProjectClosurePage/ProjectClosurePage';
import RiesgosPage from './pages/RiesgosPage/RiesgosPage';
import CallbackPage from './pages/CallbackPage/CallbackPage';
import LoggedOutPage from './pages/LoggedOutPage/LoggedOutPage';
import SecurityConfigPage from './pages/SecurityConfigPage/SecurityConfigPage';
import AccessDeniedPage from './pages/AccessDeniedPage/AccessDeniedPage';
import './App.css';

const DefaultEntryRoute = () => {
  const { assignedProjects, hasPermission, hasRole, isAdminLocal, transversal, backendLoading } = useAuthContext();

  // Esperar a que el backend termine de cargar permisos y proyectos asignados.
  // Sin esto, el rol DIRECTOR_PROYECTO ve pantalla en blanco porque se evalúan
  // los permisos antes de que llegue la respuesta de /authz/me y /usuarios/me.
  if (backendLoading) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '60vh',
        flexDirection: 'column',
        gap: '12px',
        color: '#64748b',
      }}>
        <span style={{ fontSize: '1rem' }}>Cargando perfil de usuario...</span>
      </div>
    );
  }

  const canViewDashboard = isAdminLocal
    || transversal
    || hasRole('ADMIN')
    || hasPermission('DASHBOARD:VER');
  const canViewProjects = hasRole('DIRECTOR_PROYECTO')
    || hasPermission('PROYECTO:VER')
    || (Array.isArray(assignedProjects) && assignedProjects.length > 0);

  if (canViewDashboard) {
    return <DashboardPage />;
  }

  if (canViewProjects) {
    return <Navigate to="/projects" replace />;
  }

  if (hasPermission('REPORTE:VER')) {
    return <Navigate to="/reports" replace />;
  }

  if (hasPermission('ANALITICA:VER')) {
    return <Navigate to="/analytics" replace />;
  }

  return <Navigate to="/access-denied" replace />;
};


/**
 * App Component
 * Define el sistema de rutas de la aplicación utilizando SidebarLayout como base.
 */
function App() {
  return (
    <Routes>
      <Route path="/callback" element={<CallbackPage />} />
      <Route path="/logged-out" element={<LoggedOutPage />} />
      <Route path="/access-denied" element={<AccessDeniedPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<SidebarLayout />}>
          <Route index element={<DefaultEntryRoute />} />

          <Route element={<PermissionRoute permissions={['PROYECTO:VER']} />}>
            <Route path="projects" element={<ProjectsPage />} />
          </Route>

          <Route element={<PermissionRoute permissions={['PROYECTO:CREAR']} />}>
            <Route path="proyectos/nuevo" element={<NewProjectPage />} />
          </Route>

          {/* Módulos de Proyecto */}
          <Route element={<ProjectAccessRoute />}>
            <Route path="projects/:id" element={<Outlet />}>
              <Route path="progress" element={<ProjectProgressPage />} />
              <Route path="schedule" element={<CronogramaPage />} />
              <Route path="risks" element={<RiesgosPage />} />
              <Route path="closure" element={<ProjectClosurePage />} />
            </Route>
            <Route path="proyectos/:codigoProyecto" element={<Outlet />}>
              <Route path="avance" element={<ProjectProgressPage />} />
              <Route path="riesgos" element={<RiesgosPage />} />
            </Route>
          </Route>

          <Route element={<PermissionRoute permissions={['REPORTE:VER']} />}>
            <Route path="reports" element={<ReportsPage />} />
          </Route>
          <Route element={<AnalyticsRoute />}>
            <Route path="analytics" element={<AnalyticsPage />} />
          </Route>
          <Route path="admin/configuracion" element={<AdminRoute />}>
            <Route index element={<SecurityConfigPage />} />
          </Route>
          <Route path="admin/seguridad" element={<Navigate to="/admin/configuracion" replace />} />
        </Route>
      </Route>

      <Route path="*" element={<div className="container"><h1>404 - Página no encontrada</h1></div>} />
    </Routes>
  );
}

export default App;
