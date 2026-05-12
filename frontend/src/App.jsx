import React from 'react';
import { Routes, Route } from 'react-router-dom';
import SidebarLayout from './components/layout/SidebarLayout/SidebarLayout';
import DashboardPage from './pages/DashboardPage';
import ProjectsPage from './pages/ProjectsPage';
import ReportsPage from './pages/ReportsPage';
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
        <Route path="reports" element={<ReportsPage />} />
      </Route>
      <Route path="*" element={<div className="container"><h1>404 - Página no encontrada</h1></div>} />
    </Routes>
  );
}

export default App;
