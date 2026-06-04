import { useState, useEffect } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
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
  CheckSquare,
  BarChart3
} from 'lucide-react';
import { useTheme } from '../../../context/ThemeContext';
import dashboardService from '../../../services/dashboardService';
import { useAuthContext } from '../../../context/AuthContext';
import { usePermission } from '../../../hooks/usePermission';
import './Sidebar.css';

const Sidebar = () => {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [visibleProjectCount, setVisibleProjectCount] = useState(0);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const { isDarkMode, toggleTheme } = useTheme();
  const location = useLocation();
  const { user, roles, logout, isAdminLocal, transversal, hasRole, assignedProjects } = useAuthContext();
  const canViewDashboard = usePermission('DASHBOARD:VER');
  const isDirectorProjectRole = hasRole('DIRECTOR_PROYECTO');
  const canViewProjects = usePermission('PROYECTO:VER')
    || isDirectorProjectRole
    || (Array.isArray(assignedProjects) && assignedProjects.length > 0);
  const canCloseProject = usePermission('PROYECTO:CERRAR');
  const canViewReports = usePermission('REPORTE:VER');
  const canViewAnalytics = usePermission('ANALITICA:VER');
  const canConfigureByConfigPermission = usePermission('CONFIGURACION:VER');
  const canConfigureBySystemPermission = usePermission('SISTEMA:CONFIGURAR');
  const canConfigureByPermission = canConfigureByConfigPermission || canConfigureBySystemPermission;
  const canConfigure = isAdminLocal || transversal || hasRole('ADMIN') || canConfigureByPermission;
  const isAdminLike = isAdminLocal
    || transversal
    || hasRole('ADMIN')
    || canConfigureByPermission;

  const projectMatch = location.pathname.match(/^\/(?:projects|proyectos)\/([a-zA-Z0-9-]+)/);
  const currentProjectId = projectMatch ? projectMatch[1] : null;

  useEffect(() => {
    const fetchVisibleProjectCount = async () => {
      if (isDirectorProjectRole && !isAdminLike) {
        setVisibleProjectCount(Array.isArray(assignedProjects) ? assignedProjects.length : 0);
        return;
      }

      try {
        const response = await dashboardService.getKPIs();
        if (response.success && response.data) {
          setVisibleProjectCount(response.data.total_proyectos || 0);
        }
      } catch (error) {
        console.error('Error fetching visible projects count for sidebar:', error);
      }
    };

    fetchVisibleProjectCount();
  }, [assignedProjects, isAdminLike, isDirectorProjectRole]);

  const toggleSidebar = () => {
    setIsCollapsed(!isCollapsed);
  };

  const handleLogout = async () => {
    if (isLoggingOut) {
      return;
    }

    try {
      setIsLoggingOut(true);
      await logout();
    } catch (error) {
      console.error('Error cerrando sesion:', error);
      setIsLoggingOut(false);
    }
  };

  const displayName = user?.profile?.name || user?.profile?.preferred_username || 'Usuario';
  const displayRole = roles.length ? roles.join(', ') : 'Sin rol detectado';
  const initials = displayName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || 'PR';

  const menuItems = [
    {
      category: 'PRINCIPAL',
      items: [
        canViewDashboard ? { name: 'Dashboard', path: '/', icon: <LayoutDashboard size={22} /> } : null,
        canViewProjects ? { name: 'Proyectos', path: '/projects', icon: <Briefcase size={22} />, badge: visibleProjectCount } : null,
      ]
    },
    {
      category: 'MODULOS',
      items: currentProjectId ? [
        canViewProjects ? { name: 'Avance del Proyecto', path: `/projects/${currentProjectId}/progress`, icon: <Activity size={22} /> } : null,
        canViewProjects ? { name: 'Cronograma', path: `/projects/${currentProjectId}/schedule`, icon: <Calendar size={22} /> } : null,
        canViewProjects ? { name: 'Matriz de Riesgos', path: `/projects/${currentProjectId}/risks`, icon: <AlertTriangle size={22} /> } : null,
        canCloseProject ? { name: 'Cierre del Proyecto', path: `/projects/${currentProjectId}/closure`, icon: <CheckSquare size={22} /> } : null,
      ].filter(Boolean) : []
    },
    {
      category: 'CONSULTAS',
      items: [
        canViewReports ? { name: 'Reportes', path: '/reports', icon: <FileText size={22} /> } : null,
        canViewAnalytics ? { name: 'Analíticas', path: '/analytics', icon: <BarChart3 size={22} /> } : null,
      ]
    },
    ...(canConfigure ? [{
      category: 'ADMINISTRACION',
      items: [
        { name: 'Configuracion Seguridad', path: '/admin/configuracion', icon: <ShieldCheck size={22} /> },
      ]
    }] : [])
  ].filter((group) => group.items.length > 0);

  return (
    <aside className={`sidebar ${isCollapsed ? 'collapsed' : ''}`}>
      <div className="sidebar-header">
        <div className="brand-container">
          <div className="brand-logo">
            <ShieldCheck size={24} color="#fff" />
          </div>
          {!isCollapsed && (
            <div className="brand-text">
              <span className="brand-name">PROYECTA</span>
              <span className="brand-tagline">Gestion TIC</span>
            </div>
          )}
        </div>
        <button className="toggle-btn" onClick={toggleSidebar}>
          {isCollapsed ? <ChevronRight size={18} /> : <ChevronLeft size={18} />}
        </button>
      </div>

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
                    {item.badge !== null && item.badge !== undefined ? <span className="nav-badge">{item.badge}</span> : null}
                  </>
                )}
                {isCollapsed && item.badge !== null && item.badge !== undefined ? <span className="nav-badge-dot"></span> : null}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>

      <div className="sidebar-footer">
        <div className="user-profile">
          <div className="user-avatar">{initials}</div>
          {!isCollapsed && (
            <div className="user-info">
              <span className="user-name">{displayName}</span>
              <span className="user-role">{displayRole}</span>
            </div>
          )}
        </div>

        <div className="sidebar-footer-actions">
          <button
            className="theme-toggle-btn"
            onClick={toggleTheme}
            title={isDarkMode ? 'Modo Claro' : 'Modo Oscuro'}
          >
            {isDarkMode ? <Sun size={20} /> : <Moon size={20} />}
          </button>
          <button
            className="logout-btn"
            title="Cerrar Sesion"
            onClick={handleLogout}
            disabled={isLoggingOut}
            aria-busy={isLoggingOut}
          >
            <LogOut size={20} />
          </button>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;

