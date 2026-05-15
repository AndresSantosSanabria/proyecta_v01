import React from 'react';
import { Routes, Route } from 'react-router-dom';
import SidebarLayout from './components/layout/SidebarLayout/SidebarLayout';
import DashboardPage from './pages/DashboardPage';
import ProjectsPage from './pages/ProjectsPage';
import ReportsPage from './pages/ReportsPage';
import ProjectProgressPage from './pages/ProjectProgressPage';
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
        
        {/* Módulos de Proyecto */}
        <Route path="projects/:id/progress" element={<ProjectProgressPage />} />
        <Route path="projects/:id/schedule" element={<div className="container"><h1>Cronograma en construcción</h1></div>} />
        <Route path="projects/:id/risks" element={<div className="container"><h1>Matriz de Riesgos en construcción</h1></div>} />
        <Route path="projects/:id/closure" element={<div className="container"><h1>Cierre del Proyecto en construcción</h1></div>} />

        <Route path="reports" element={<ReportsPage />} />
      </Route>
      <Route path="*" element={<div className="container"><h1>404 - Página no encontrada</h1></div>} />
    </Routes>
  );
}

export default App;
