import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import ErrorBoundary from '../../common/ErrorBoundary';
import './SidebarLayout.css';

/**
 * SidebarLayout Component
 * Estructura base de la aplicación con Sidebar persistente.
 * Utiliza Outlet para renderizar las páginas dinámicamente.
 * Incluye ErrorBoundary para capturar errores en las páginas hijas.
 */
const SidebarLayout = () => {
  return (
    <div className="sidebar-layout">
      <Sidebar />
      <main className="content-area">
        <ErrorBoundary>
          <Outlet />
        </ErrorBoundary>
      </main>
    </div>
  );
};

export default SidebarLayout;
