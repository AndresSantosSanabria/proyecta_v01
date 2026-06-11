import { ShieldCheck, Mail, UserCircle2, BriefcaseBusiness } from 'lucide-react';
import { useAuthContext } from '../../context/AuthContext';
import './ProfilePage.css';

const ProfilePage = () => {
  const { user, backendProfile, roles, primaryRole, assignedProjects } = useAuthContext();
  const displayName = user?.profile?.name || backendProfile?.nombre || user?.profile?.preferred_username || 'Usuario';
  const email = user?.profile?.email || backendProfile?.correo || 'Sin correo';
  const dependency = backendProfile?.dependencia || 'Sin dependencia';

  return (
    <div className="compact-page profile-page">
      <header className="profile-page__header">
        <div>
          <h1 className="page-title">Perfil</h1>
          <p className="page-subtitle">Resumen compacto de identidad, roles y asignaciones.</p>
        </div>
        <span className="soft-pill">{primaryRole || 'Sin rol principal'}</span>
      </header>

      <section className="profile-card card-surface">
        <div className="profile-card__avatar">
          <UserCircle2 size={28} />
        </div>
        <div className="profile-card__body">
          <strong>{displayName}</strong>
          <span>{email}</span>
        </div>
      </section>

      <section className="profile-meta-grid">
        <article className="profile-meta">
          <ShieldCheck size={16} />
          <div>
            <span>Roles</span>
            <strong>{roles.length ? roles.join(', ') : 'Sin roles'}</strong>
          </div>
        </article>
        <article className="profile-meta">
          <Mail size={16} />
          <div>
            <span>Correo</span>
            <strong>{email}</strong>
          </div>
        </article>
        <article className="profile-meta">
          <BriefcaseBusiness size={16} />
          <div>
            <span>Dependencia</span>
            <strong>{dependency}</strong>
          </div>
        </article>
        <article className="profile-meta">
          <UserCircle2 size={16} />
          <div>
            <span>Proyectos</span>
            <strong>{Array.isArray(assignedProjects) ? assignedProjects.length : 0}</strong>
          </div>
        </article>
      </section>
    </div>
  );
};

export default ProfilePage;
