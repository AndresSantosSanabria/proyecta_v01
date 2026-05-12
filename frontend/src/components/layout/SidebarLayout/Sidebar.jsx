import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Briefcase, 
  FileText, 
  LogOut, 
  ChevronLeft, 
  ChevronRight, 
  ShieldCheck 
} from 'lucide-react';
import './Sidebar.css';

/**
 * Sidebar Component
 * Maneja la navegación principal y el estado de colapso.
 * Sigue los requerimientos de diseño de la imagen proporcionada.
 */
const Sidebar = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);

  const toggleSidebar = () => {
    setIsCollapsed(!isCollapsed);
  };

  const menuItems = [
    {
      category: 'PRINCIPAL',
      items: [
        { name: 'Dashboard', path: '/', icon: <LayoutDashboard size={22} /> },
        { name: 'Proyectos', path: '/projects', icon: <Briefcase size={22} />, badge: 2 },
      ]
    },
    {
      category: 'MÓDULOS',
      items: []
    },
    {
      category: 'CONSULTAS',
      items: [
        { name: 'Reportes', path: '/reports', icon: <FileText size={22} /> },
      ]
    }
  ];

  return (
    <aside className={`sidebar ${isCollapsed ? 'collapsed' : ''}`}>
      {/* Branding Section */}
      <div className="sidebar-header">
        <div className="brand-container">
          <div className="brand-logo">
            <ShieldCheck size={24} color="#fff" />
          </div>
          {!isCollapsed && (
            <div className="brand-text">
              <span className="brand-name">PROYECTA</span>
              <span className="brand-tagline">Gestión TIC</span>
            </div>
          )}
        </div>
        <button className="toggle-btn" onClick={toggleSidebar}>
          {isCollapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
        </button>
      </div>

      {/* Navigation Menu */}
      <nav className="sidebar-nav">
        {menuItems.map((group, idx) => (
          <div key={idx} className="nav-group">
            {!isCollapsed && <h3 className="nav-category">{group.category}</h3>}
            {group.items.map((item) => (
              <NavLink 
                key={item.name} 
                to={item.path} 
                className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
              >
                <span className="nav-icon">{item.icon}</span>
                {!isCollapsed && (
                  <>
                    <span className="nav-text">{item.name}</span>
                    {item.badge && <span className="nav-badge">{item.badge}</span>}
                  </>
                )}
                {isCollapsed && item.badge && <span className="nav-badge-dot"></span>}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>

      {/* User Profile Section */}
      <div className="sidebar-footer">
        <div className="user-profile">
          <div className="user-avatar">AD</div>
          {!isCollapsed && (
            <div className="user-info">
              <span className="user-name">Administrador</span>
              <span className="user-role">Gestor de Proyectos TI</span>
            </div>
          )}
        </div>
        <button className="logout-btn" title="Cerrar Sesión">
          <LogOut size={20} />
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
