import React, { useState, useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Briefcase, 
  FileText, 
  LogOut, 
  ChevronLeft, 
  ChevronRight, 
  ShieldCheck,
  Sun,
  Moon,
  Activity,
  Calendar,
  AlertTriangle,
  CheckSquare
} from 'lucide-react';
import { useTheme } from '../../../context/ThemeContext';
import { useLocation } from 'react-router-dom';

import dashboardService from '../../../services/dashboardService';
import './Sidebar.css';

const Sidebar = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [activeCount, setActiveCount] = useState(0);
  const { isDarkMode, toggleTheme } = useTheme();
  const location = useLocation();

  // Detectar si estamos dentro de un proyecto específico
  const projectMatch = location.pathname.match(/^\/projects\/([a-zA-Z0-9-]+)/);
  const currentProjectId = projectMatch ? projectMatch[1] : null;

  useEffect(() => {
    const fetchActiveCount = async () => {
      try {
        const response = await dashboardService.getKPIs();
        if (response.success && response.data) {
          // Usamos el conteo de proyectos activos del sistema
          setActiveCount(response.data.activos || 0);
        }
      } catch (error) {
        console.error('Error fetching active projects count for sidebar:', error);
      }
    };

    fetchActiveCount();
  }, []);

  const toggleSidebar = () => {
    setIsCollapsed(!isCollapsed);
  };

  const menuItems = [
    {
      category: 'PRINCIPAL',
      items: [
        { name: 'Dashboard', path: '/', icon: <LayoutDashboard size={22} /> },
        { name: 'Proyectos', path: '/projects', icon: <Briefcase size={22} />, badge: activeCount },
      ]
    },
    {
      category: 'MÓDULOS',
      items: currentProjectId ? [
        { name: 'Avance del Proyecto', path: `/projects/${currentProjectId}/progress`, icon: <Activity size={22} /> },
        { name: 'Cronograma', path: `/projects/${currentProjectId}/schedule`, icon: <Calendar size={22} /> },
        { name: 'Matriz de Riesgos', path: `/projects/${currentProjectId}/risks`, icon: <AlertTriangle size={22} /> },
        { name: 'Cierre del Proyecto', path: `/projects/${currentProjectId}/closure`, icon: <CheckSquare size={22} /> },
      ] : []
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
        
        <div className="sidebar-footer-actions">
          <button 
            className="theme-toggle-btn" 
            onClick={toggleTheme} 
            title={isDarkMode ? "Modo Claro" : "Modo Oscuro"}
          >
            {isDarkMode ? <Sun size={20} /> : <Moon size={20} />}
          </button>
          <button className="logout-btn" title="Cerrar Sesión">
            <LogOut size={20} />
          </button>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
