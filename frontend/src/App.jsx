import { useEffect, useRef } from 'react';
import { Routes, Route, Navigate, Outlet } from 'react-router-dom';
import SidebarLayout from './components/layout/SidebarLayout/SidebarLayout';
import ProtectedRoute from './components/ProtectedRoute/ProtectedRoute';
import PermissionRoute from './components/ProtectedRoute/PermissionRoute';
import AdminRoute from './components/ProtectedRoute/AdminRoute';
import AnalyticsRoute from './components/ProtectedRoute/AnalyticsRoute';
import ProjectAccessRoute from './components/ProtectedRoute/ProjectAccessRoute';
import ProjectLifecycleGuard from './components/projects/ProjectLifecycleGuard';
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
import SecurityConfigPage from './pages/SecurityConfigPage/SecurityConfigPage';
import { hasProjectScopePermission, hasAdminScopePermission } from './utils/permissions';
import { startLoginRedirect } from './utils/auth';
import './App.css';

const LoadingRedirectState = ({ title, subtitle }) => (
  <div style={{
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: '60vh',
    flexDirection: 'column',
    gap: '12px',
    color: '#64748b',
  }}>
    <span style={{ fontSize: '1rem' }}>{title}</span>
    {subtitle ? <span style={{ fontSize: '0.92rem' }}>{subtitle}</span> : null}
  </div>
);

const DefaultEntryRoute = () => {
  const {
    assignedProjects,
    hasPermission,
    hasRole,
    isAdminLocal,
    transversal,
    backendLoading,
    permissions,
  } = useAuthContext();
  const loginTriggeredRef = useRef(false);

  const canViewDashboard = !hasRole('DIRECTOR_PROYECTO') && (
    isAdminLocal
    || transversal
    || hasRole('ADMIN')
    || hasPermission('DASHBOARD:VER')
  );
  const canViewProjects = hasRole('DIRECTOR_PROYECTO')
    || hasProjectScopePermission(permissions)
    || hasPermission('PROYECTO:VER')
    || (Array.isArray(assignedProjects) && assignedProjects.length > 0);
  const canViewReports = hasPermission('REPORTE:VER');
  const canViewAnalytics = hasPermission('ANALITICA:VER');
  const canViewAdmin = hasAdminScopePermission(permissions);
  const shouldRedirectToLogin = !backendLoading
    && !canViewDashboard
    && !canViewProjects
    && !canViewReports
    && !canViewAnalytics
    && !canViewAdmin;

  useEffect(() => {
    if (!shouldRedirectToLogin || loginTriggeredRef.current) {
      return;
    }

    loginTriggeredRef.current = true;
    startLoginRedirect().catch((error) => {
      loginTriggeredRef.current = false;
      console.error('No fue posible redirigir al login:', error);
    });
  }, [shouldRedirectToLogin]);

  if (backendLoading) {
    return <LoadingRedirectState title="Cargando perfil de usuario..." />;
  }

  if (canViewDashboard) {
    return <DashboardPage />;
  }

  if (canViewProjects) {
    return <Navigate to="/projects" replace />;
  }

  if (canViewReports) {
    return <Navigate to="/reports" replace />;
  }

  if (canViewAnalytics) {
    return <Navigate to="/analytics" replace />;
  }

  if (canViewAdmin) {
    return <Navigate to="/admin/configuracion" replace />;
  }

  return (
    <LoadingRedirectState
      title="Redirigiendo a Keycloak..."
      subtitle="No tienes acceso efectivo desde la matriz de permisos, asi que te enviaremos al login."
    />
  );
};

/**
 * App Component
 * Define el sistema de rutas de la aplicacion utilizando SidebarLayout como base.
 */
function App() {
  return (
    <Routes>
      <Route path="/callback" element={<CallbackPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<SidebarLayout />}>
          <Route index element={<DefaultEntryRoute />} />

          <Route element={<PermissionRoute permissions={['PROYECTO:VER']} />}>
            <Route path="projects" element={<ProjectsPage />} />
          </Route>

          <Route element={<PermissionRoute permissions={['PROYECTO:CREAR']} />}>
            <Route path="proyectos/nuevo" element={<NewProjectPage />} />
          </Route>

          <Route element={<ProjectAccessRoute />}>
            <Route element={<ProjectLifecycleGuard />}>
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

      <Route path="*" element={<div className="container"><h1>404 - Pagina no encontrada</h1></div>} />
    </Routes>
  );
}

export default App;
