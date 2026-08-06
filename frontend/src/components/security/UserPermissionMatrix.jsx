import { useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  BadgeCheck,
  Check,
  ChevronDown,
  ChevronRight,
  Eye,
  Loader2,
  Save,
  ShieldCheck,
  User,
  X,
} from 'lucide-react';
import securityService from '../../services/securityService';
import './UserPermissionMatrix.css';

const SIDEBAR_CATEGORY = 'Modulos Sidebar';

const SIDEBAR_ITEM_LABELS = {
  'SIDEBAR:DASHBOARD': 'Dashboard',
  'SIDEBAR:PROYECTOS': 'Proyectos',
  'SIDEBAR:REPORTES': 'Reportes',
  'SIDEBAR:ANALITICAS': 'Analiticas',
  'SIDEBAR:SEGURIDAD': 'Configuracion Seguridad',
};

const CATEGORY_SORT_ORDER = [SIDEBAR_CATEGORY, 'Analiticas', 'Avances', 'Beneficio e Impacto', 'Cierre', 'Configuracion', 'Cronograma', 'Dashboard', 'Documentos', 'Entregables', 'Evidencias', 'Proyectos', 'Reportes', 'Sistema'];

const UserPermissionMatrix = ({ onClose }) => {
  const [users, setUsers] = useState([]);
  const [selectedUserId, setSelectedUserId] = useState(null);
  const [matrix, setMatrix] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [expandedCategories, setExpandedCategories] = useState(new Set());
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    loadUsers();
  }, []);

  useEffect(() => {
    if (selectedUserId) {
      loadMatrix(selectedUserId);
    }
  }, [selectedUserId]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const raw = await securityService.listUsers({ size: 200 });
      const payload = raw?.data?.data ?? raw?.data ?? raw;
      const list = Array.isArray(payload) ? payload : payload?.content || payload?.items || [];
      setUsers(deduplicateUsers(list));
    } catch {
      setError('Error al cargar usuarios.');
    } finally {
      setLoading(false);
    }
  };

  const deduplicateUsers = (list) => {
    const byName = new Map();
    for (const u of list) {
      const normalizedName = (u.nombre || '').trim().toLowerCase();
      const normalizedEmail = (u.correo || '').trim().toLowerCase();
      const key = normalizedName || normalizedEmail || String(u.id);

      const existing = byName.get(key);
      if (!existing) {
        byName.set(key, u);
      } else {
        const existingHasEmail = (existing.correo || '').includes('@');
        const currentHasEmail = (normalizedEmail || '').includes('@');
        if (currentHasEmail && !existingHasEmail) {
          byName.set(key, u);
        }
      }
    }
    return Array.from(byName.values());
  };

  const loadMatrix = async (usuarioId) => {
    try {
      setLoading(true);
      setError('');
      setNotice('');
      const raw = await securityService.getUserPermissionMatrix(usuarioId);
      const result = raw?.data?.data ?? raw?.data ?? raw;
      setMatrix(result);
      const cats = new Set(result.permisos?.map((p) => p.categoria) || []);
      setExpandedCategories(cats);
    } catch {
      setError('Error al cargar la matriz de permisos.');
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = (permisoId, concedido) => {
    if (!matrix) return;
    setMatrix((prev) => ({
      ...prev,
      permisos: prev.permisos.map((p) =>
        p.permisoId === permisoId ? { ...p, concedido, sidebar: concedido, source: true } : p
      ),
    }));
  };

  const handleToggleCategory = (categoria, value) => {
    if (!matrix) return;
    setMatrix((prev) => ({
      ...prev,
      permisos: prev.permisos.map((p) =>
        p.categoria === categoria ? { ...p, concedido: value, sidebar: value, source: true } : p
      ),
    }));
  };

  const handleSave = async () => {
    if (!matrix) return;
    try {
      setSaving(true);
      setError('');
      setNotice('');
      await securityService.saveUserPermissionMatrix({
        usuarioId: matrix.usuarioId,
        permisos: matrix.permisos.map((p) => ({
          permisoId: p.permisoId,
          concedido: p.concedido,
        })),
      });
      setNotice('Permisos guardados correctamente.');
      window.dispatchEvent(new Event('proyecta:authz:refresh'));
    } catch {
      setError('Error al guardar los permisos.');
    } finally {
      setSaving(false);
    }
  };

  const toggleCategory = (cat) => {
    setExpandedCategories((prev) => {
      const next = new Set(prev);
      if (next.has(cat)) next.delete(cat);
      else next.add(cat);
      return next;
    });
  };

  const categories = useMemo(() => {
    if (!matrix?.permisos) return [];
    const catMap = {};
    for (const p of matrix.permisos) {
      if (!catMap[p.categoria]) catMap[p.categoria] = [];
      catMap[p.categoria].push(p);
    }
    return Object.entries(catMap).sort(([a], [b]) => {
      const ia = CATEGORY_SORT_ORDER.indexOf(a);
      const ib = CATEGORY_SORT_ORDER.indexOf(b);
      const va = ia >= 0 ? ia : 999;
      const vb = ib >= 0 ? ib : 999;
      return va - vb || a.localeCompare(b);
    });
  }, [matrix]);

  const filteredUsers = users.filter((u) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      (u.username || '').toLowerCase().includes(term) ||
      (u.nombre || '').toLowerCase().includes(term) ||
      (u.correo || '').toLowerCase().includes(term)
    );
  });

  const totalGranted = matrix?.permisos?.filter((p) => p.concedido).length || 0;
  const totalOverridden = matrix?.permisos?.filter((p) => p.source).length || 0;

  return (
    <article className="panel panel-main upm-panel">
      <div className="panel-topbar">
        <div>
          <p className="security-eyebrow">CONFIGURACION DEL SISTEMA</p>
          <h2>Matriz de Permisos por Usuario</h2>
        </div>
        <div className="upm-topbar-actions">
          <button type="button" className="btn-secondary" onClick={onClose}>
            <X size={14} />
            Cerrar
          </button>
        </div>
      </div>

      <div className="upm-body">
        {error && (
          <div className="feedback-banner error">
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}
        {notice && (
          <div className="feedback-banner success">
            <BadgeCheck size={16} />
            <span>{notice}</span>
          </div>
        )}

        <div className="upm-layout">
          <div className="upm-user-list">
            <div className="upm-search">
              <input
                type="text"
                placeholder="Buscar usuario..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="upm-search-input"
              />
            </div>
            <div className="upm-users-scroll">
              {filteredUsers.map((u) => (
                <button
                  key={u.id}
                  type="button"
                  className={`upm-user-item ${selectedUserId === u.id ? 'active' : ''}`}
                  onClick={() => setSelectedUserId(u.id)}
                >
                  <User size={14} />
                  <div className="upm-user-item-info">
                    <span className="upm-user-item-name">{u.nombre || u.username}</span>
                    <span className="upm-user-item-role">{u.correo || u.username}</span>
                  </div>
                </button>
              ))}
            </div>
          </div>

          <div className="upm-matrix-container">
            {!selectedUserId && (
              <div className="upm-empty">
                <ShieldCheck size={40} strokeWidth={1} />
                <p>Seleccione un usuario para ver y editar su matriz de permisos.</p>
              </div>
            )}

            {selectedUserId && loading && (
              <div className="upm-loading">
                <Loader2 size={20} className="animate-spin" />
                <span>Cargando matriz...</span>
              </div>
            )}

            {selectedUserId && !loading && matrix && (
              <>
                <div className="upm-matrix-header">
                  <div className="upm-matrix-user-info">
                    <User size={16} />
                    <span className="upm-matrix-username">{matrix.username}</span>
                    <span className="upm-matrix-rol">
                      <ShieldCheck size={12} /> {matrix.rolCodigo || 'Sin rol'}
                    </span>
                  </div>
                  <div className="upm-matrix-stats">
                    <span>{totalGranted} permisos activos</span>
                    {totalOverridden > 0 && (
                      <span className="upm-overridden-badge">{totalOverridden} personalizados</span>
                    )}
                    <button
                      type="button"
                      className="btn-primary"
                      onClick={handleSave}
                      disabled={saving}
                    >
                      {saving ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />}
                      Guardar
                    </button>
                  </div>
                </div>

                <div className="upm-matrix-table-wrapper">
                  <table className="upm-matrix-table">
                    <thead>
                      <tr>
                        <th className="upm-th-categoria">Permiso</th>
                        <th className="upm-th-check">
                          <span className="upm-th-label">Concedido</span>
                        </th>
                        <th className="upm-th-source">Origen</th>
                        <th className="upm-th-sidebar">Sidebar</th>
                        <th className="upm-th-accion">Accion</th>
                      </tr>
                    </thead>
                    <tbody>
                      {categories.map(([categoria, permisos]) => {
                        const allChecked = permisos.every((p) => p.concedido);
                        const someChecked = permisos.some((p) => p.concedido);
                        const isExpanded = expandedCategories.has(categoria);
                        const isSidebarCategory = categoria === SIDEBAR_CATEGORY;

                        return (
                          <Fragment key={categoria}>
                            <tr className={`upm-category-row ${isSidebarCategory ? 'sidebar-category' : ''}`}>
                              <td colSpan={5} onClick={() => toggleCategory(categoria)}>
                                <div className="upm-category-header">
                                  {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                                  <span className="upm-category-name">{categoria}</span>
                                  {isSidebarCategory && (
                                    <span className="upm-visibility-badge">
                                      <Eye size={12} /> VISIBILIDAD
                                    </span>
                                  )}
                                  <span className="upm-category-count">
                                    {permisos.filter((p) => p.concedido).length}/{permisos.length}
                                  </span>
                                  <button
                                    type="button"
                                    className={`upm-toggle-all ${allChecked ? 'active' : someChecked ? 'partial' : ''}`}
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      handleToggleCategory(categoria, !allChecked);
                                    }}
                                  >
                                    {allChecked ? 'Todos' : someChecked ? 'Algunos' : 'Ninguno'}
                                  </button>
                                </div>
                              </td>
                            </tr>
                            {isExpanded &&
                              permisos.map((p) => (
                                <tr
                                  key={p.permisoId}
                                  className={`upm-permission-row ${p.source ? 'overridden' : ''} ${isSidebarCategory ? 'sidebar-perm' : ''}`}
                                >
                                  <td className="upm-td-codigo">
                                    <span className="upm-perm-code">{p.codigo}</span>
                                    <span className="upm-perm-name">{p.nombre}</span>
                                  </td>
                                  <td className="upm-td-check">
                                    <label className="upm-checkbox">
                                      <input
                                        type="checkbox"
                                        checked={p.concedido}
                                        onChange={(e) => handleToggle(p.permisoId, e.target.checked)}
                                      />
                                      <span className="upm-checkmark">
                                        {p.concedido && <Check size={10} />}
                                      </span>
                                    </label>
                                  </td>
                                  <td className="upm-td-source">
                                    {p.source ? (
                                      <span className="upm-source-override">Personalizado</span>
                                    ) : p.concedido ? (
                                      <span className="upm-source-role">Rol</span>
                                    ) : (
                                      <span className="upm-source-none">-</span>
                                    )}
                                  </td>
                                  <td className="upm-td-sidebar">
                                    {isSidebarCategory ? (
                                      <label className="upm-toggle-switch">
                                        <input
                                          type="checkbox"
                                          checked={p.sidebar}
                                          onChange={(e) => handleToggle(p.permisoId, e.target.checked)}
                                        />
                                        <span className="upm-toggle-slider" />
                                      </label>
                                    ) : SIDEBAR_ITEM_LABELS[p.codigo] ? (
                                      <span className="upm-sidebar-tag">{SIDEBAR_ITEM_LABELS[p.codigo]}</span>
                                    ) : (
                                      <span className="upm-sidebar-none">--</span>
                                    )}
                                  </td>
                                  <td className="upm-td-accion">
                                    {isSidebarCategory ? (
                                      <span className={`upm-accion-badge ${p.sidebar ? 'visible' : 'hidden'}`}>
                                        {p.sidebar ? 'Visible' : 'Oculto'}
                                      </span>
                                    ) : (
                                      <span className={`upm-accion-badge ${p.concedido ? 'active' : 'inactive'}`}>
                                        {p.concedido ? 'Activo' : 'Inactivo'}
                                      </span>
                                    )}
                                  </td>
                                </tr>
                              ))}
                          </Fragment>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </article>
  );
};

const Fragment = ({ children }) => children;

export default UserPermissionMatrix;
