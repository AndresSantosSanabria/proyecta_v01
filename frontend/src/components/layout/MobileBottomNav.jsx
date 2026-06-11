import { BarChart3, BellRing, House, UserCircle2 } from 'lucide-react';
import { NavLink } from 'react-router-dom';

const items = [
  { path: '/', label: 'Home', icon: House },
  { path: '/analytics', label: 'Analítica', icon: BarChart3 },
  { path: '/notifications', label: 'Notif.', icon: BellRing },
  { path: '/profile', label: 'Perfil', icon: UserCircle2 },
];

const MobileBottomNav = () => {
  return (
    <nav className="mobile-bottom-nav mobile-only" aria-label="Navegación principal">
      {items.map(({ path, label, icon: Icon }) => (
        <NavLink
          key={path}
          to={path}
          className={({ isActive }) => `mobile-bottom-nav__item ${isActive ? 'active' : ''}`}
        >
          <Icon size={18} />
          <span>{label}</span>
        </NavLink>
      ))}
    </nav>
  );
};

export default MobileBottomNav;
