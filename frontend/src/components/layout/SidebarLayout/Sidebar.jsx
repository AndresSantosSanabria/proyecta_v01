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
  BarChart3,
  ClipboardCheck,
} from 'lucide-react';
import { useTheme } from '../../../context/ThemeContext';
import dashboardService from '../../../services/dashboardService';
import { useAuthContext } from '../../../context/AuthContext';
import { usePermission } from '../../../hooks/usePermission';
import NotificationBell from '../NotificationBell';
import './Sidebar.css';

const Sidebar = ({ mobileOpen = false, onMobileClose }) => {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [visibleProjectCount, setVisibleProjectCount] = useState(0);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const { isDarkMode, toggleTheme } = useTheme();
  const location = useLocation();
  const { user, roles, primaryRole, backendProfile, formatRoleLabel, logout, assignedProjects } = useAuthContext();
  const canViewDashboard = usePermission('DASHBOARD:VER');
  const canViewProjects = usePermission('PROYECTO:VER')
    || (Array.isArray(assignedProjects) && assignedProjects.length > 0);
  const canCloseProject = usePermission('PROYECTO:CERRAR');
  const canViewReports = usePermission('REPORTE:VER');
  const canViewAnalytics = usePermission('ANALITICA:VER');
  const canConfigure = usePermission('CONFIGURACION:VER') || usePermission('SISTEMA:CONFIGURAR');
  const canViewAllProjects = usePermission('PROYECTO:VER_TODOS');

  const sidebarDashboard = usePermission('SIDEBAR:DASHBOARD');
  const sidebarProyectos = usePermission('SIDEBAR:PROYECTOS');
  const sidebarReportes = usePermission('SIDEBAR:REPORTES');
  const sidebarAnaliticas = usePermission('SIDEBAR:ANALITICAS');
  const sidebarSeguridad = usePermission('SIDEBAR:SEGURIDAD');

  const projectMatch = location.pathname.match(/^\/(?:projects|proyectos)\/([a-zA-Z0-9-]+)/);
  const currentProjectId = projectMatch ? projectMatch[1] : null;

  useEffect(() => {
    const fetchVisibleProjectCount = async () => {
      if (!canViewAllProjects) {
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
  }, [assignedProjects, canViewAllProjects]);

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

  const isValidRoleStr = (r) => {
    if (!r) return false;
    const str = String(r).trim().toUpperCase();
    return str !== '' && str !== 'SIN ROL' && str !== 'SIN_ROL' && str !== 'UNDEFINED' && str !== 'NULL';
  };

  const displayName = backendProfile?.nombre || user?.profile?.name || user?.profile?.preferred_username || 'Usuario';
  const roleCandidates = [
    backendProfile?.rolNombre,
    backendProfile?.rol_nombre,
    backendProfile?.rolCodigo,
    backendProfile?.rol,
    primaryRole,
    Array.isArray(roles) ? roles.find((r) => r && String(r).toUpperCase() !== 'VISUALIZADOR') : null,
    Array.isArray(roles) ? roles[0] : null,
    (Array.isArray(assignedProjects) && assignedProjects.length > 0) ? 'DIRECTOR_PROYECTO' : null,
  ];

  const rawRole = roleCandidates.find(isValidRoleStr) || 'Visualizador';
  const displayRole = formatRoleLabel ? formatRoleLabel(rawRole) : rawRole;
  const initials = displayName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || 'PR';

  const compactItems = (items) => items.filter(Boolean);
  const closeMobileMenu = () => {
    if (mobileOpen && onMobileClose) {
      onMobileClose();
    }
  };

  const menuItems = [
    {
      category: 'PRINCIPAL',
      items: compactItems([
        (sidebarDashboard) ? { name: 'Dashboard', path: '/', icon: <LayoutDashboard size={22} /> } : null,
        (sidebarProyectos) ? { name: 'Proyectos', path: '/projects', icon: <Briefcase size={22} />, badge: visibleProjectCount } : null,
      ])
    },
    {
      category: 'MODULOS',
      items: currentProjectId ? [
        (sidebarProyectos) ? { name: 'Avance del Proyecto', path: `/projects/${currentProjectId}/progress`, icon: <Activity size={22} /> } : null,
        (sidebarProyectos) ? { name: 'Cronograma', path: `/projects/${currentProjectId}/schedule`, icon: <Calendar size={22} /> } : null,
        (sidebarProyectos) ? { name: 'Matriz de Riesgos', path: `/projects/${currentProjectId}/risks`, icon: <AlertTriangle size={22} /> } : null,
        (sidebarProyectos) ? { name: 'Evidencias', path: `/projects/${currentProjectId}/evidences`, icon: <ClipboardCheck size={22} /> } : null,
        (sidebarProyectos) ? { name: 'Cierre del Proyecto', path: `/projects/${currentProjectId}/closure`, icon: <CheckSquare size={22} /> } : null,
      ].filter(Boolean) : []
    },
    {
      category: 'CONSULTAS',
      items: compactItems([
        (sidebarReportes) ? { name: 'Reportes', path: '/reports', icon: <FileText size={22} /> } : null,
        (sidebarAnaliticas) ? { name: 'Analíticas', path: '/analytics', icon: <BarChart3 size={22} /> } : null,
      ])
    },
    ...((sidebarSeguridad) ? [{
      category: 'ADMINISTRACION',
      items: compactItems([
        { name: 'Configuracion Seguridad', path: '/admin/configuracion', icon: <ShieldCheck size={22} /> },
      ])
    }] : [])
  ].filter((group) => group.items.length > 0);

  return (
    <>
      <button
        type="button"
        className={`sidebar-backdrop mobile-only ${mobileOpen ? 'open' : ''}`}
        onClick={onMobileClose}
        aria-label="Cerrar menú"
      />
      <aside className={`sidebar ${isCollapsed ? 'collapsed' : ''} ${mobileOpen ? 'mobile-open' : ''}`}>
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
        <button className="toggle-btn" onClick={toggleSidebar} aria-label={isCollapsed ? 'Expandir barra' : 'Colapsar barra'}>
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
                onClick={closeMobileMenu}
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
        {!isCollapsed && (
          <div className="user-profile">
            <div className="user-avatar">{initials}</div>
            <div className="user-info">
              <span className="user-name" title={displayName}>{displayName}</span>
              {displayRole ? <span className="user-role" title={displayRole}>{displayRole}</span> : null}
            </div>
          </div>
        )}

        <div className="sidebar-footer-actions">
          {isCollapsed && <div className="user-avatar user-avatar--collapsed" title={displayName}>{initials}</div>}
          <NotificationBell />
          <button
            className="theme-toggle-btn"
            onClick={toggleTheme}
            title={isDarkMode ? 'Modo Claro' : 'Modo Oscuro'}
          >
            {isDarkMode ? <Sun size={20} /> : <Moon size={20} />}
          </button>
          <button
            className="logout-btn"
            title="Cerrar Sesión"
            onClick={handleLogout}
            disabled={isLoggingOut}
            aria-busy={isLoggingOut}
          >
            <LogOut size={20} />
          </button>
        </div>
      </div>
    </aside>
    </>
  );
};

export default Sidebar;

