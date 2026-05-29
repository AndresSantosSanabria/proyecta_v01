import { Routes, Route, Navigate, Outlet } from 'react-router-dom';
import SidebarLayout from './components/layout/SidebarLayout/SidebarLayout';
import ProtectedRoute from './components/ProtectedRoute/ProtectedRoute';
import AdminRoute from './components/ProtectedRoute/AdminRoute';
import AnalyticsRoute from './components/ProtectedRoute/AnalyticsRoute';
import ProjectAccessRoute from './components/ProtectedRoute/ProjectAccessRoute';
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
          <Route index element={<DashboardPage />} />
          <Route path="projects" element={<ProjectsPage />} />
          <Route path="proyectos/nuevo" element={<NewProjectPage />} />

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

          <Route path="reports" element={<ReportsPage />} />
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
