import React from 'react';
import { Routes, Route } from 'react-router-dom';
import SidebarLayout from './components/layout/SidebarLayout/SidebarLayout';
import DashboardPage from './pages/DashboardPage/DashboardPage';
import ProjectsPage from './pages/ProjectsPage/ProjectsPage';
import ReportsPage from './pages/ReportsPage/ReportsPage';
import NewProjectPage from './pages/NewProjectPage/NewProjectPage';
import ProjectProgressPage from './pages/ProjectProgressPage/ProjectProgressPage';
import CronogramaPage from './pages/CronogramaPage/CronogramaPage';
import ProjectClosurePage from './pages/ProjectClosurePage/ProjectClosurePage';
import './App.css';

/**
 * App Component
 * Define el sistema de rutas de la aplicación utilizando SidebarLayout como base.
 */
function App() {
  return (
    <Routes>
      <Route path="/" element={<SidebarLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="projects" element={<ProjectsPage />} />
        <Route path="proyectos/nuevo" element={<NewProjectPage />} />
        
        {/* Módulos de Proyecto */}
        <Route path="projects/:id/progress" element={<ProjectProgressPage />} />
        <Route path="proyectos/:codigoProyecto/avance" element={<ProjectProgressPage />} />
        <Route path="projects/:id/schedule" element={<CronogramaPage />} />
        <Route path="projects/:id/risks" element={<div className="container"><h1>Matriz de Riesgos en construcción</h1></div>} />
        <Route path="projects/:id/closure" element={<ProjectClosurePage />} />

        <Route path="reports" element={<ReportsPage />} />
      </Route>
      <Route path="*" element={<div className="container"><h1>404 - Página no encontrada</h1></div>} />
    </Routes>
  );
}

export default App;
