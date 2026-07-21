import { Outlet, useLocation } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { Menu } from 'lucide-react';
import Sidebar from './Sidebar';
import ErrorBoundary from '../../common/ErrorBoundary';
import NotificationBell from '../NotificationBell';
import ToastHost from '../ToastHost';
import { useAuthContext } from '../../../context/AuthContext';
import './SidebarLayout.css';

const SidebarLayout = () => {
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { user, primaryRole } = useAuthContext();

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMobileMenuOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!mobileMenuOpen) return undefined;

    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        setMobileMenuOpen(false);
      }
    };

    document.body.classList.add('modal-open');
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.classList.remove('modal-open');
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [mobileMenuOpen]);

  return (
    <div className="sidebar-layout screen-shell">
      <header className="mobile-topbar mobile-only">
        <button
          type="button"
          className="icon-btn mobile-topbar__menu"
          onClick={() => setMobileMenuOpen(true)}
          aria-label="Abrir menu"
          aria-expanded={mobileMenuOpen}
        >
          <Menu size={18} />
        </button>
        <div className="mobile-topbar__copy">
          <span className="mobile-topbar__title">PROYECTA</span>
          <span className="mobile-topbar__subtitle">{primaryRole || user?.profile?.preferred_username || 'Usuario'}</span>
        </div>
        <div className="mobile-topbar__actions">
          <NotificationBell />
        </div>
      </header>
      <div className="sidebar-layout__body">
        <Sidebar mobileOpen={mobileMenuOpen} onMobileClose={() => setMobileMenuOpen(false)} />
        <main className="content-area screen-scroll">
          <ErrorBoundary>
            <Outlet />
          </ErrorBoundary>
        </main>
      </div>
      <ToastHost />
    </div>
  );
};

export default SidebarLayout;
