import { Briefcase, House, ShieldCheck } from 'lucide-react';
import { NavLink } from 'react-router-dom';

const items = [
  { path: '/', label: 'Inicio', icon: House },
  { path: '/projects', label: 'Proyectos', icon: Briefcase },
  { path: '/admin/configuracion', label: 'Ajustes', icon: ShieldCheck },
];

const MobileBottomNav = () => {
  return (
    <nav className="mobile-bottom-nav mobile-only" aria-label="Navegacion principal">
      {items.map(({ path, label, icon: Icon }) => (
        <NavLink
          key={path}
          to={path}
          className={({ isActive }) => `mobile-bottom-nav__item ${isActive ? 'active' : ''}`}
        >
          <Icon size={20} strokeWidth={2.1} />
          <span>{label}</span>
        </NavLink>
      ))}
    </nav>
  );
};

export default MobileBottomNav;
