import { Outlet, useLocation } from 'react-router-dom';
import { useEffect, useState } from 'react';
import Sidebar from './Sidebar';
import ErrorBoundary from '../../common/ErrorBoundary';
import CompactTopBar from '../CompactTopBar';
import MobileBottomNav from '../MobileBottomNav';
import './SidebarLayout.css';

const SidebarLayout = () => {
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

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
      <CompactTopBar onMenuToggle={() => setMobileMenuOpen((current) => !current)} />
      <Sidebar mobileOpen={mobileMenuOpen} onMobileClose={() => setMobileMenuOpen(false)} />
      <main className="content-area screen-scroll">
        <ErrorBoundary>
          <Outlet />
        </ErrorBoundary>
      </main>
      <MobileBottomNav />
    </div>
  );
};

export default SidebarLayout;
