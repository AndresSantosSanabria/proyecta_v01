import { Menu, ShieldCheck, MoonStar, SunMedium } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';

const CompactTopBar = ({ onMenuToggle }) => {
  const { isDarkMode, toggleTheme } = useTheme();

  return (
    <header className="mobile-topbar mobile-only">
      <div className="mobile-topbar__brand">
        <span className="mobile-topbar__shield" aria-hidden="true">
          <ShieldCheck size={18} />
        </span>
        <div className="mobile-topbar__copy">
          <span className="mobile-topbar__title">PROYECTA</span>
          <span className="mobile-topbar__subtitle">Gestión TIC</span>
        </div>
      </div>

      <div className="mobile-topbar__actions">
        <button type="button" className="icon-btn" onClick={toggleTheme} aria-label="Cambiar tema">
          {isDarkMode ? <SunMedium size={16} /> : <MoonStar size={16} />}
        </button>
        <button type="button" className="icon-btn" onClick={onMenuToggle} aria-label="Abrir menú">
          <Menu size={16} />
        </button>
      </div>
    </header>
  );
};

export default CompactTopBar;
