import { Routes, Route } from 'react-router-dom';
import SidebarLayout from './components/layout/SidebarLayout/SidebarLayout';
import DashboardPage from './pages/DashboardPage/DashboardPage';
import ProjectsPage from './pages/ProjectsPage/ProjectsPage';
import ReportsPage from './pages/ReportsPage/ReportsPage';
import NewProjectPage from './pages/NewProjectPage/NewProjectPage';
import ProjectProgressPage from './pages/ProjectProgressPage/ProjectProgressPage';
import CronogramaPage from './pages/CronogramaPage/CronogramaPage';
import ProjectClosurePage from './pages/ProjectClosurePage/ProjectClosurePage';
import CallbackPage from './pages/CallbackPage/CallbackPage';
import LoggedOutPage from './pages/LoggedOutPage/LoggedOutPage';
import ProtectedRoute from './components/ProtectedRoute/ProtectedRoute';
import './App.css';

/**
 * App Component
 * Define el sistema de rutas de la aplicaciÃ³n utilizando SidebarLayout como base.
 */
function App() {
  return (
    <Routes>
      <Route path="/callback" element={<CallbackPage />} />
      <Route path="/logged-out" element={<LoggedOutPage />} />

      {/* Rutas Privadas Protegidas (OCP) */}
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<SidebarLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="projects" element={<ProjectsPage />} />
        <Route path="proyectos/nuevo" element={<NewProjectPage />} />
        
        {/* MÃ³dulos de Proyecto */}
        <Route path="projects/:id/progress" element={<ProjectProgressPage />} />
        <Route path="proyectos/:codigoProyecto/avance" element={<ProjectProgressPage />} />
        <Route path="projects/:id/schedule" element={<CronogramaPage />} />
        <Route path="projects/:id/risks" element={<div className="container"><h1>Matriz de Riesgos en construcciÃ³n</h1></div>} />
        <Route path="projects/:id/closure" element={<ProjectClosurePage />} />

        <Route path="reports" element={<ReportsPage />} />
      </Route>
      </Route>
      <Route path="*" element={<div className="container"><h1>404 - PÃ¡gina no encontrada</h1></div>} />
    </Routes>
  );
}

export default App;

