import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ShieldCheck,
  KeyRound,
  DatabaseZap,
  BadgeCheck,
  Search,
  Plus,
  Save,
  RefreshCw,
  CircleAlert,
  Users,
  LockKeyhole,
  Activity,
  Mail,
  UserCog,
} from 'lucide-react';
import { useAuthContext } from '../../context/AuthContext';
import securityService from '../../services/securityService';
import './SecurityConfigPage.css';

const defaultForm = {
  nombre: '',
  correo: '',
  rolCodigo: '',
  activo: true,
};

const formatDateTime = (value) => {
  if (!value) {
    return 'No registra';
  }

  try {
    return new Intl.DateTimeFormat('es-CO', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(value));
  } catch {
    return value;
  }
};

const SecurityConfigPage = () => {
  const { user, roles: tokenRoles } = useAuthContext();
  const [profile, setProfile] = useState(null);
  const [roles, setRoles] = useState([]);
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState('');
  const [selectedUserId, setSelectedUserId] = useState(null);
  const [form, setForm] = useState(defaultForm);

  const activeRoles = useMemo(
    () => roles.filter((role) => role.activo !== false),
    [roles]
  );

  const loadConfiguration = useCallback(async (searchValue = '') => {
    try {
      setLoading(true);
      setError(null);

      const [profileResponse, rolesResponse, usersResponse] = await Promise.all([
        securityService.getCurrentProfile(),
        securityService.getActiveRoles(),
        securityService.getUsers({ search: searchValue, size: 50 }),
      ]);

      setProfile(profileResponse?.data ?? null);
      setRoles(Array.isArray(rolesResponse?.data) ? rolesResponse.data : []);

      const userPage = usersResponse?.data;
      const content = Array.isArray(userPage?.content) ? userPage.content : [];
      setUsers(content);
    } catch (fetchError) {
      console.error('Error cargando configuracion de seguridad:', fetchError);
      setError('No fue posible cargar la configuracion de seguridad desde el backend.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      loadConfiguration('').catch(console.error);
    }, 0);

    return () => window.clearTimeout(timer);
  }, [loadConfiguration]);

  const handleSearch = async (event) => {
    event.preventDefault();
    await loadConfiguration(search);
  };

  const handleReload = async () => {
    await loadConfiguration(search);
  };

  const handleReset = () => {
    setSelectedUserId(null);
    setForm(defaultForm);
    setNotice('');
  };

  const openEditor = (item) => {
    setSelectedUserId(item.id);
    setForm({
      nombre: item.nombre || '',
      correo: item.correo || '',
      rolCodigo: item.rolCodigo || '',
      activo: Boolean(item.activo),
    });
    setNotice('');
    setError(null);
  };

  const handleChange = (field) => (event) => {
    const value = field === 'activo' ? event.target.checked : event.target.value;
    setForm((current) => ({
      ...current,
      [field]: value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!form.nombre.trim() || !form.correo.trim() || !form.rolCodigo.trim()) {
      setError('Debes completar nombre, correo y rol antes de guardar.');
      return;
    }

    try {
      setSaving(true);
      setError(null);
      setNotice('');

      const payload = {
        nombre: form.nombre.trim(),
        correo: form.correo.trim().toLowerCase(),
        rolCodigo: form.rolCodigo.trim(),
        activo: Boolean(form.activo),
      };

      if (selectedUserId) {
        await securityService.updateUser(selectedUserId, payload);
        setNotice('Usuario actualizado correctamente.');
      } else {
        await securityService.createUser(payload);
        setNotice('Usuario creado correctamente.');
      }

      await loadConfiguration(search);
      setSelectedUserId(null);
      setForm(defaultForm);
    } catch (saveError) {
      console.error('Error guardando usuario:', saveError);
      setError('No fue posible guardar el usuario. Revisa la informacion y vuelve a intentar.');
    } finally {
      setSaving(false);
    }
  };

  const quickStats = [
    {
      label: 'Usuarios cargados',
      value: users.length,
      icon: <Users size={20} />,
      tone: 'primary',
    },
    {
      label: 'Roles activos',
      value: activeRoles.length,
      icon: <BadgeCheck size={20} />,
      tone: 'success',
    },
    {
      label: 'Sesión actual',
      value: profile?.nombre || user?.profile?.name || 'Sin datos',
      icon: <Activity size={20} />,
      tone: 'warning',
    },
  ];

  return (
    <div className="security-page">
      <section className="security-hero">
        <div className="hero-copy">
          <span className="eyebrow">
            <LockKeyhole size={16} />
            Seguridad institucional
          </span>
          <h1>Configuración de acceso, roles y validación granular</h1>
          <p>
            Keycloak autentica al usuario por correo y contraseña. Luego el backend valida
            el correo contra la tabla local, activa el rol correcto y habilita la sesión solo
            si el usuario está registrado y activo.
          </p>
          <div className="hero-actions">
            <button className="btn-primary" onClick={handleReload} disabled={loading}>
              <RefreshCw size={16} />
              Refrescar datos
            </button>
            <button className="btn-secondary" onClick={handleReset}>
              <Plus size={16} />
              Nuevo usuario local
            </button>
          </div>
        </div>

        <div className="hero-panel">
          <div className="panel-header">
            <ShieldCheck size={22} />
            <span>Cadena de confianza</span>
          </div>
          <div className="trust-chain">
            <div className="trust-step">
              <KeyRound size={18} />
              <div>
                <strong>Keycloak</strong>
                <p>Autentica el correo institucional.</p>
              </div>
            </div>
            <div className="trust-step">
              <DatabaseZap size={18} />
              <div>
                <strong>Backend</strong>
                <p>Busca el correo en la tabla `usuario`.</p>
              </div>
            </div>
            <div className="trust-step">
              <UserCog size={18} />
              <div>
                <strong>Rol final</strong>
                <p>Asigna el permiso según `rol_config`.</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="stats-grid">
        {quickStats.map((item) => (
          <article key={item.label} className={`stat-card tone-${item.tone}`}>
            <div className="stat-icon">{item.icon}</div>
            <div>
              <span>{item.label}</span>
              <strong>{item.value}</strong>
            </div>
          </article>
        ))}
      </section>

      {error && (
        <div className="message-box error">
          <CircleAlert size={18} />
          <span>{error}</span>
        </div>
      )}

      {notice && (
        <div className="message-box success">
          <BadgeCheck size={18} />
          <span>{notice}</span>
        </div>
      )}

      <section className="config-grid">
        <article className="config-card profile-card">
          <div className="section-title">
            <div>
              <h2>Usuario autenticado</h2>
              <p>Información que entrega el backend luego de validar Keycloak.</p>
            </div>
            <span className="status-pill ok">Validado</span>
          </div>

          <div className="profile-summary">
            <div className="avatar-large">
              {(profile?.nombre || user?.profile?.name || 'U')
                .split(' ')
                .filter(Boolean)
                .slice(0, 2)
                .map((part) => part[0]?.toUpperCase())
                .join('')}
            </div>
            <div>
              <h3>{profile?.nombre || user?.profile?.name || 'Usuario autenticado'}</h3>
              <p>{profile?.correo || user?.profile?.email || 'Sin correo'}</p>
              <small>Roles del token: {tokenRoles.length ? tokenRoles.join(', ') : 'sin datos'}</small>
            </div>
          </div>

          <div className="meta-list">
            <div>
              <span>Rol backend</span>
              <strong>{profile?.rol || 'No definido'}</strong>
            </div>
            <div>
              <span>Dependencia</span>
              <strong>{profile?.dependencia || 'No definida'}</strong>
            </div>
            <div>
              <span>Ultimo acceso</span>
              <strong>{formatDateTime(profile?.ultimoAcceso)}</strong>
            </div>
          </div>
        </article>

        <article className="config-card roles-card">
          <div className="section-title">
            <div>
              <h2>Roles granulares</h2>
              <p>Catálogo vivo de `rol_config` desde el backend.</p>
            </div>
          </div>

          <div className="role-grid">
            {activeRoles.map((role) => (
              <div key={role.id || role.codigo} className="role-card">
                <div className="role-card-header">
                  <strong>{role.nombre}</strong>
                  <span className={`status-pill ${role.codigo === 'ADMINISTRADOR' ? 'warn' : 'ok'}`}>
                    {role.codigo}
                  </span>
                </div>
                <p>{role.descripcion || 'Sin descripcion'}</p>
                <div className="role-footer">
                  <span>Nivel {role.nivelAcceso}</span>
                  <span>{role.activo ? 'Activo' : 'Inactivo'}</span>
                </div>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section className="config-card">
        <div className="section-title">
          <div>
            <h2>Usuarios autorizados</h2>
            <p>Alta, edición y cambio de estado sobre la tabla granular del backend.</p>
          </div>

          <form className="search-form" onSubmit={handleSearch}>
            <Search size={16} />
            <input
              type="text"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Buscar por nombre o correo"
            />
            <button type="submit" className="btn-secondary compact">
              Buscar
            </button>
          </form>
        </div>

        {loading ? (
          <div className="empty-state">Cargando configuración de seguridad...</div>
        ) : (
          <div className="users-layout">
            <div className="users-table-wrap">
              <table className="users-table">
                <thead>
                  <tr>
                    <th>Usuario</th>
                    <th>Correo</th>
                    <th>Rol</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {users.length === 0 ? (
                    <tr>
                      <td colSpan="5">
                        <div className="empty-state">
                          No hay usuarios que coincidan con la búsqueda.
                        </div>
                      </td>
                    </tr>
                  ) : (
                    users.map((item) => (
                      <tr key={item.id} className={item.id === selectedUserId ? 'active-row' : ''}>
                        <td>
                          <strong>{item.nombre}</strong>
                          <small>{formatDateTime(item.fechaCreacion)}</small>
                        </td>
                        <td>{item.correo}</td>
                        <td>
                          <div className="role-cell">
                            <span>{item.rolNombre || item.rolCodigo}</span>
                            <small>{item.rolCodigo}</small>
                          </div>
                        </td>
                        <td>
                          <span className={`status-pill ${item.activo ? 'ok' : 'danger'}`}>
                            {item.activo ? 'Activo' : 'Inactivo'}
                          </span>
                        </td>
                        <td>
                          <div className="table-actions">
                            <button
                              type="button"
                              className="btn-link"
                              onClick={() => openEditor(item)}
                            >
                              Editar
                            </button>
                            <button
                              type="button"
                              className="btn-link"
                              onClick={async () => {
                                setSaving(true);
                                setError(null);
                                setNotice('');
                                try {
                                  await securityService.toggleUserState(item.id, !item.activo);
                                  setNotice(
                                    item.activo ? 'Usuario desactivado correctamente.' : 'Usuario activado correctamente.'
                                  );
                                  await loadConfiguration(search);
                                } catch (toggleError) {
                                  console.error('Error cambiando estado:', toggleError);
                                  setError('No fue posible cambiar el estado del usuario.');
                                } finally {
                                  setSaving(false);
                                }
                              }}
                              disabled={saving}
                            >
                              {item.activo ? 'Desactivar' : 'Activar'}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <aside className="editor-card">
              <div className="section-title compact-title">
                <div>
                  <h3>{selectedUserId ? 'Editar usuario' : 'Crear usuario local'}</h3>
                  <p>
                    {selectedUserId
                      ? 'Actualiza el rol granular y el estado.'
                      : 'Crea un usuario que luego debe existir también en Keycloak.'}
                  </p>
                </div>
              </div>

              <form className="editor-form" onSubmit={handleSubmit}>
                <label>
                  <span>Nombre</span>
                  <input
                    type="text"
                    value={form.nombre}
                    onChange={handleChange('nombre')}
                    placeholder="Fabio Andres Santos Sanabria"
                  />
                </label>

                <label>
                  <span>Correo institucional</span>
                  <div className="input-with-icon">
                    <Mail size={16} />
                    <input
                      type="email"
                      value={form.correo}
                      onChange={handleChange('correo')}
                      placeholder="fabio.santos@cundinamarca.gov.co"
                      disabled={Boolean(selectedUserId)}
                    />
                  </div>
                </label>

                <label>
                  <span>Rol granular</span>
                  <select value={form.rolCodigo} onChange={handleChange('rolCodigo')}>
                    <option value="">Seleccionar rol</option>
                    {roles.map((role) => (
                      <option key={role.codigo} value={role.codigo}>
                        {role.nombre} ({role.codigo})
                      </option>
                    ))}
                  </select>
                </label>

                <label className="switch-row">
                  <span>Usuario activo</span>
                  <input
                    type="checkbox"
                    checked={form.activo}
                    onChange={handleChange('activo')}
                  />
                </label>

                <div className="editor-actions">
                  <button type="submit" className="btn-primary" disabled={saving}>
                    <Save size={16} />
                    {saving ? 'Guardando...' : selectedUserId ? 'Guardar cambios' : 'Crear usuario'}
                  </button>
                  <button type="button" className="btn-secondary" onClick={handleReset}>
                    Limpiar
                  </button>
                </div>
              </form>
            </aside>
          </div>
        )}
      </section>
    </div>
  );
};

export default SecurityConfigPage;
