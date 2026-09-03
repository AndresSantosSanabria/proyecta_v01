import { useEffect, useMemo, useRef, useState } from 'react';
import {
  BadgeCheck,
  Ban,
  CircleAlert,
  Clock3,
  Briefcase,
  FileText,
  FolderOpen,
  List,
  ListTodo,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Search,
  ShieldCheck,
  BellRing,
  X,
  Users,
} from 'lucide-react';
import { useAuthContext } from '../../context/AuthContext';
import { usePermission } from '../../hooks/usePermission';
import projectService from '../../services/projectService';
import securityService from '../../services/securityService';
import configCatalogService from '../../services/configCatalogService';
import NotificationTemplatesPanel from '../../components/security/NotificationTemplatesPanel';
import ClosureTemplatePanel from '../../components/security/ClosureTemplatePanel';
import ClosureQuestionsPanel from '../../components/security/ClosureQuestionsPanel';
import SpellCheckerTextarea from '../../components/common/SpellCheckerTextarea';
import SpellCheckerInput from '../../components/common/SpellCheckerInput';
import { AutocompleteSelect } from '../../components/common/AutocompleteSelect';
import ListaParametricaPanel from '../../components/security/ListaParametricaPanel';
import StorageConfigPanel from '../../components/security/StorageConfigPanel';
import UserPermissionMatrix from '../../components/security/UserPermissionMatrix';
import './SecurityConfigPage.css';

const SECURITY_TABS = {
  USERS: 'usuarios',
  ROLES: 'roles',
  PARAMETERS: 'parametros',
  ASSIGNMENTS: 'asignaciones',
  NOTIFICATIONS: 'notificaciones',
  CLOSURE_TEMPLATE: 'acta_cierre',
  CLOSURE_QUESTIONS: 'preguntas_cierre',
  LISTAS_PARAMETRICAS: 'listas_parametricas',
  STORAGE: 'almacenamiento',
  USER_PERMISSIONS: 'permisos_usuario',
};

const emptyUserForm = {
  username: '',
  nombre: '',
  correo: '',
  dependencia: '',
  rol: '',
  activo: true,
};

const emptyRoleForm = {
  codigo: '',
  nombre: '',
  descripcion: '',
  transversal: false,
  activo: true,
};

const ACTION_ORDER = [
  'VER',
  'LISTAR',
  'CONSULTAR',
  'DESCARGAR',
  'CREAR',
  'REGISTRAR',
  'CARGAR',
  'SUBIR',
  'EDITAR',
  'ACTUALIZAR',
  'MODIFICAR',
  'SOLICITAR',
  'APROBAR',
  'CERRAR',
  'ELIMINAR',
  'CONFIGURAR',
];

const roleLabels = {
  DASHBOARD: 'Dashboard',
  PROYECTO: 'Gestión de Proyectos',
  REPORTE: 'Reportes',
  ANALITICA: 'Analíticas',
  ENTREGABLE: 'Entregables',
  AVANCE: 'Avance del Proyecto',
  EVIDENCIA: 'Evidencias',
  DOCUMENTO: 'Documentos',
  CRONOGRAMA: 'Cronograma',
  CIERRE: 'Cierre del Proyecto',
  CONFIGURACION: 'Configuracion',
  SISTEMA: 'Administracion del Sistema',
  OTROS: 'Otros permisos',
};

const roleDisplayLabels = {
  ADMIN: 'Administrador',
  DIRECTOR_PROYECTO: 'Director de Proyecto',
  DIRECTOR_TECNICO: 'Director Técnico',
  LIDER_TECNICO: 'Líder Técnico',
  GESTOR_TIC: 'Gestor TIC',
  CONSULTA: 'Consulta',
};

const normalizeRoleValue = (value) => {
  if (!value) return '';
  if (Array.isArray(value)) return value[0] || '';
  return value.toString().split(',')[0].trim();
};

const normalizeComparableRole = (value) => String(value || '')
  .trim()
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .toUpperCase()
  .replace(/^ROLE[\s_-]+/, '')
  .replace(/[^A-Z0-9]+/g, '_')
  .replace(/^_+|_+$/g, '');

const formatRoleLabel = (value) => {
  const normalized = normalizeRoleValue(value);
  if (!normalized) return '';
  return roleDisplayLabels[normalized] || normalized
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
};

const formatAssignmentCargoLabel = (value) => formatRoleLabel(value);

const isDirectorEquivalentRole = (value) => ['DIRECTOR_PROYECTO', 'LIDER_TECNICO', 'DIRECTOR_TECNICO']
  .includes(normalizeComparableRole(value));

const roleTokensMatch = (sourceValue, roleValue) => {
  const source = normalizeComparableRole(sourceValue);
  const candidate = normalizeComparableRole(roleValue);
  if (!source || !candidate) return false;

  if (source === candidate) {
    return true;
  }

  const sourceTokens = source.split('_').filter(Boolean);
  const candidateTokens = candidate.split('_').filter(Boolean);
  if (sourceTokens.length === 0 || candidateTokens.length === 0) {
    return false;
  }

  return sourceTokens.every((token) => candidateTokens.includes(token))
    || candidateTokens.every((token) => sourceTokens.includes(token));
};

const resolveClosestExistingRole = (sources, availableRoles) => {
  const sourceList = Array.isArray(sources) ? sources : [sources];
  const rolesList = Array.isArray(availableRoles) ? availableRoles : [];

  if (rolesList.length === 0) {
    return null;
  }

  for (const sourceValue of sourceList) {
    const foundRole = rolesList.find((role) => roleTokensMatch(sourceValue, role?.codigo) || roleTokensMatch(sourceValue, role?.nombre));
    if (foundRole) {
      return foundRole;
    }
  }

  return null;
};

const formatDateTime = (value) => {
  if (!value) return 'Sin registro';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value.toString();
  }

  return new Intl.DateTimeFormat('es-CO', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
};

const getUserRoleCode = (user, fallbackRole = '') => normalizeRoleValue(user?.rolCodigo || user?.rol || user?.role || user?.roles || '') || fallbackRole;

const getUserRoleLabel = (user, fallbackRole = '') => user?.rolNombre || user?.rolLabel || formatRoleLabel(user?.rolCodigo || user?.rol || user?.role || user?.roles || '') || formatRoleLabel(fallbackRole);

const getExplicitUserRoleSources = (user) => [
  user?.rolCodigo,
  user?.rol,
  user?.role,
  user?.roles,
  user?.rolNombre,
  user?.rolLabel,
]
  .flatMap((value) => {
    if (Array.isArray(value)) {
      return value.flatMap((item) => (typeof item === 'object' && item !== null ? [item.codigo, item.nombre] : item));
    }

    if (typeof value === 'object' && value !== null) {
      return [value.codigo, value.nombre];
    }

    return [value];
  })
  .map((value) => String(value || '').trim())
  .filter(Boolean);

const getUserRoleSearchSources = (user) => [
  ...getExplicitUserRoleSources(user),
  user?.username,
  user?.correo,
  user?.nombre,
  user?.email,
]
  .map((value) => String(value || '').trim())
  .filter(Boolean);

const isSameUsername = (left, right) => {
  if (!left || !right) return false;
  return left.toString().trim().toLowerCase() === right.toString().trim().toLowerCase();
};

const getUserLastAccess = (user) => (
  user?.ultimoAcceso
  || user?.lastLogin
  || user?.lastAccess
  || user?.fechaUltimoAcceso
  || user?.updatedAt
  || user?.fechaActualizacion
  || null
);

const getAvatarColor = (name) => {
  const palette = ['#2563eb', '#0f766e', '#7c3aed', '#d97706', '#059669', '#dc2626', '#0ea5e9', '#9333ea'];
  if (!name) return palette[0];

  let hash = 0;
  for (let i = 0; i < name.length; i += 1) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }

  return palette[Math.abs(hash) % palette.length];
};

const mapUserToForm = (user) => ({
  username: user?.username || '',
  nombre: user?.nombre || '',
  correo: user?.correo || '',
  dependencia: user?.dependencia || '',
  rol: getUserRoleCode(user),
  activo: Boolean(user?.activo),
});

const groupPermissions = (permissions) => {
  const grouped = permissions.reduce((accumulator, permission) => {
    const [groupKey = 'OTROS', actionKey = 'OTRO'] = (permission.codigo || '').split(':');
    if (!accumulator[groupKey]) {
      accumulator[groupKey] = [];
    }
    accumulator[groupKey].push({ ...permission, actionKey });
    return accumulator;
  }, {});

  const order = Object.keys(roleLabels);

  return Object.entries(grouped)
    .sort(([left], [right]) => {
      const leftIndex = order.includes(left) ? order.indexOf(left) : order.length;
      const rightIndex = order.includes(right) ? order.indexOf(right) : order.length;
      return leftIndex - rightIndex || left.localeCompare(right);
    })
    .map(([groupKey, groupPermissionsList]) => ({
      key: groupKey,
      label: roleLabels[groupKey] || groupKey,
      permissions: groupPermissionsList.sort((left, right) => {
        const leftIndex = ACTION_ORDER.indexOf(left.actionKey);
        const rightIndex = ACTION_ORDER.indexOf(right.actionKey);
        if (leftIndex === -1 && rightIndex === -1) return left.nombre.localeCompare(right.nombre);
        if (leftIndex === -1) return 1;
        if (rightIndex === -1) return -1;
        return leftIndex - rightIndex || left.nombre.localeCompare(right.nombre);
      }),
    }));
};

const emptyMessage = (title, detail) => (
  <div className="empty-state">
    <strong>{title}</strong>
    <span>{detail}</span>
  </div>
);

const extractApiDetail = (error) => {
  return error?.response?.data?.detail
    || error?.response?.data?.title
    || error?.message
    || '';
};

const unwrapPayload = (value) => value?.data?.data ?? value?.data ?? value;

const extractCollection = (value) => {
  const payload = unwrapPayload(value);

  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.data)) return payload.data;

  return [];
};

const normalizeAssignmentCargos = (value) => {
  const payload = unwrapPayload(value);
  const cargos = Array.isArray(payload) ? payload : String(payload || '').split(',');

  return [...new Set(
    cargos
      .map((cargo) => String(cargo || '').trim())
      .filter(Boolean)
  )];
};

const getProjectId = (project) => project?.codigo || project?.id || project?.proyectoId || project?.proyecto_id || '';
const getProjectName = (project) => project?.nombre || project?.nombreProyecto || project?.name || getProjectId(project);

const SecurityConfigPage = () => {
  const { accessToken } = useAuthContext();

  const [activeSection, setActiveSection] = useState(SECURITY_TABS.USERS);
  const [roles, setRoles] = useState([]);
  const [parametricRoles, setParametricRoles] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [users, setUsers] = useState([]);
  const [projects, setProjects] = useState([]);
  const [assignmentCargos, setAssignmentCargos] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [usersLoadError, setUsersLoadError] = useState('');
  const [assignmentLoading, setAssignmentLoading] = useState(false);
  const [savingAssignment, setSavingAssignment] = useState(false);
  const [userSearch, setUserSearch] = useState('');
  const [roleSearch, setRoleSearch] = useState('');
  const [assignmentForm, setAssignmentForm] = useState({
    username: '',
    proyectoId: '',
    cargo: '',
  });

  const [selectedUser, setSelectedUser] = useState(null);
  const [creatingUser, setCreatingUser] = useState(false);
  const [userForm, setUserForm] = useState(emptyUserForm);
  const [savingUser, setSavingUser] = useState(false);

  const [selectedRoleCode, setSelectedRoleCode] = useState('');
  const [creatingRole, setCreatingRole] = useState(false);
  const [roleEditorOpen, setRoleEditorOpen] = useState(false);
  const [roleTemplateCode, setRoleTemplateCode] = useState('');
  const [roleForm, setRoleForm] = useState(emptyRoleForm);
  const [roleDraftPermissions, setRoleDraftPermissions] = useState(new Set());
  const [savingRole, setSavingRole] = useState(false);
  const [savingPermissions, setSavingPermissions] = useState(false);
  const assignmentsSectionRef = useRef(null);

  const canConfigure = usePermission('CONFIGURACION:VER') || usePermission('SISTEMA:CONFIGURAR');
  const selectedRole = useMemo(
    () => roles.find((role) => role.codigo === selectedRoleCode) || null,
    [roles, selectedRoleCode]
  );

  const permissionGroups = useMemo(() => groupPermissions(permissions), [permissions]);
  const isUserEditorOpen = Boolean(selectedUser) && !creatingUser;
  const selectedAssignmentUser = useMemo(
    () => users.find((user) => user.username === assignmentForm.username) || null,
    [assignmentForm.username, users]
  );
  const selectedAssignmentUserRole = getUserRoleCode(selectedAssignmentUser);
  const shouldRestrictToDirectors = isDirectorEquivalentRole(assignmentForm.cargo);
  const selectedAssignmentUserRoleKey = selectedAssignmentUserRole.toUpperCase();
  const isAssignmentsModalOpen = canConfigure && activeSection === SECURITY_TABS.ASSIGNMENTS;
  const isFullscreenModalOpen = isUserEditorOpen || isAssignmentsModalOpen;
  const assignableUsers = (() => {
    if (!shouldRestrictToDirectors) {
      return users;
    }

    return users.filter((user) => isDirectorEquivalentRole(getUserRoleCode(user)));
  })();
  const projectOptions = useMemo(
    () => [...projects].sort((left, right) => {
      const leftName = getProjectName(left);
      const rightName = getProjectName(right);
      return leftName.localeCompare(rightName, 'es');
    }),
    [projects]
  );

  const filteredUsers = useMemo(() => {
    const query = userSearch.trim().toLowerCase();
    if (!query) return users;
    return users.filter((user) => {
      const haystack = [user.nombre, user.username, user.correo, user.dependencia, getUserRoleCode(user), getUserRoleLabel(user)]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return haystack.includes(query);
    });
  }, [users, userSearch]);

  const filteredRoles = useMemo(() => {
    const query = roleSearch.trim().toLowerCase();
    if (!query) return roles;
    return roles.filter((role) => {
      const haystack = [role.nombre, role.codigo, role.descripcion].filter(Boolean).join(' ').toLowerCase();
      return haystack.includes(query);
    });
  }, [roles, roleSearch]);

  const loadData = async (search = '') => {
    try {
      setLoading(true);
      setError('');

      const [rolesResult, permissionsResult, usersResult, projectsResult, cargosResult, parametricRolesResult] = await Promise.allSettled([
        securityService.listRoles({ includeInactive: true }),
        securityService.listPermissions(),
        securityService.listUsers({ search, size: 100 }),
        projectService.getAllUnpaged(),
        securityService.listAssignmentCargos(),
        configCatalogService.listarParametrica('ROL_USUARIO', true),
      ]);

      const rolesData = rolesResult.status === 'fulfilled' ? extractCollection(rolesResult.value) : [];
      const permissionsData = permissionsResult.status === 'fulfilled' ? extractCollection(permissionsResult.value) : [];
      const usersData = usersResult.status === 'fulfilled' ? extractCollection(usersResult.value) : [];
      const projectsData = projectsResult.status === 'fulfilled' && Array.isArray(projectsResult.value)
        ? projectsResult.value
        : [];
      const cargosData = cargosResult.status === 'fulfilled' ? normalizeAssignmentCargos(cargosResult.value) : [];
      const parametricRolesData = parametricRolesResult.status === 'fulfilled'
        ? (Array.isArray(parametricRolesResult.value) ? parametricRolesResult.value : [])
        : [];
      const resolvedUsersData = usersData.map((item) => {
        const explicitRoleObject = resolveClosestExistingRole(getUserRoleSearchSources(item), rolesData);
        const resolvedRoleObject = explicitRoleObject;
        const resolvedRoleCode = resolvedRoleObject?.codigo || '';

        if (!resolvedRoleCode) {
          return item;
        }

        const resolvedRoleLabel = resolvedRoleObject?.nombre || formatRoleLabel(resolvedRoleCode);

        return {
          ...item,
          rolCodigo: item?.rolCodigo || resolvedRoleCode,
          rol: item?.rol || resolvedRoleCode,
          role: item?.role || resolvedRoleCode,
          roles: item?.roles || [resolvedRoleCode],
          rolNombre: item?.rolNombre || resolvedRoleLabel,
          rolLabel: item?.rolLabel || resolvedRoleLabel,
        };
      });

      const usersFetchFailed = usersResult.status === 'rejected';
      setUsersLoadError(
        usersFetchFailed
          ? `No se pudo cargar la lista de usuarios. ${extractApiDetail(usersResult.reason)}`
          : ''
      );

      setRoles(rolesData);
      setParametricRoles(parametricRolesData);
      setPermissions(permissionsData);
      setUsers(resolvedUsersData);
      setProjects(projectsData);
      setAssignmentCargos(cargosData);

      if (!creatingRole) {
        const nextRole = (selectedRoleCode && rolesData.find((role) => role.codigo === selectedRoleCode))
          || rolesData[0]
          || null;

        if (nextRole) {
          setSelectedRoleCode(nextRole.codigo);
          setRoleForm({
            codigo: nextRole.codigo || '',
            nombre: nextRole.nombre || '',
            descripcion: nextRole.descripcion || '',
            transversal: Boolean(nextRole.transversal),
            activo: Boolean(nextRole.activo),
          });
          setRoleDraftPermissions(new Set((nextRole.permisos || []).map((permiso) => permiso.codigo)));
        } else {
          setSelectedRoleCode('');
          setRoleForm(emptyRoleForm);
          setRoleDraftPermissions(new Set());
        }
      }

      if (selectedUser?.username) {
        const freshUser = resolvedUsersData.find((item) => item.username === selectedUser.username) || null;
        if (freshUser) {
          setSelectedUser(freshUser);
          setUserForm({
            ...mapUserToForm(freshUser),
            rol: getUserRoleCode(freshUser),
          });
        } else {
          setSelectedUser(null);
          setUserForm(emptyUserForm);
        }
      }

      if (canConfigure) {
        const backfillCandidates = usersData
          .map((item) => {
            if (getUserRoleCode(item)) {
              return null;
            }

            return {
              username: item?.username || '',
              nombre: item?.nombre || '',
              correo: item?.correo || '',
              dependencia: item?.dependencia || '',
              rol: 'visualizador',
              activo: Boolean(item?.activo),
            };
          })
          .filter(Boolean);

        if (backfillCandidates.length > 0) {
          await Promise.allSettled(
            backfillCandidates.map((payload) => securityService.updateUser(payload))
          );
        }
      }
    } catch (fetchError) {
      console.error('Error cargando configuracion de seguridad:', fetchError);
      setError('No fue posible cargar la configuracion de seguridad.');
    } finally {
      setLoading(false);
    }
  };

  const loadAssignments = async (username) => {
    if (!username) {
      setAssignments([]);
      return;
    }

    try {
      setAssignmentLoading(true);
      const response = await securityService.listAssignments(username);
      setAssignments(extractCollection(response));
    } catch (assignmentFetchError) {
      console.error('Error cargando asignaciones:', assignmentFetchError);
      setAssignments([]);
      setError('No fue posible cargar las asignaciones del usuario seleccionado.');
    } finally {
      setAssignmentLoading(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData().catch(console.error);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!assignmentForm.username) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setAssignments([]);
      return undefined;
    }

    loadAssignments(assignmentForm.username).catch(console.error);
    return undefined;
  }, [assignmentForm.username]);

  useEffect(() => {
    if (!assignmentForm.username) {
      return;
    }

    const isStillAssignable = assignableUsers.some((user) => user.username === assignmentForm.username);
    if (!isStillAssignable) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setAssignmentForm((current) => ({
        ...current,
        username: '',
      }));
    }
  }, [assignableUsers, assignmentForm.username]);

  useEffect(() => {
    if (!isFullscreenModalOpen) {
      document.body.classList.remove('modal-open');
      return undefined;
    }

    document.body.classList.add('modal-open');
    const handleKeyDown = (event) => {
      if (event.key !== 'Escape') {
        return;
      }

      if (isAssignmentsModalOpen) {
        setActiveSection(SECURITY_TABS.USERS);
        return;
      }

      if (isUserEditorOpen) {
        setCreatingUser(false);
        setSelectedUser(null);
        setUserForm(emptyUserForm);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      document.body.classList.remove('modal-open');
    };
  }, [isAssignmentsModalOpen, isFullscreenModalOpen, isUserEditorOpen]);

  const handleReload = () => {
    loadData(userSearch).catch(console.error);
  };

  const handleSearch = async (event) => {
    event.preventDefault();
    await loadData(userSearch);
  };

  const handleUserFieldChange = (field) => (event) => {
    const value = field === 'activo' ? event.target.checked : event.target.value;
    setUserForm((current) => ({ ...current, [field]: value }));
  };

  const handleRoleFieldChange = (field) => (event) => {
    const value = field === 'transversal' || field === 'activo' ? event.target.checked : event.target.value;
    setRoleForm((current) => ({ ...current, [field]: value }));
  };

  const handleAssignmentFieldChange = (field) => (event) => {
    const value = event.target.value;
    setAssignmentForm((current) => ({ ...current, [field]: value }));
  };

  const handleSelectUser = (user) => {
    setCreatingUser(false);
    setSelectedUser(user);
    setUserForm({
      ...mapUserToForm(user),
      rol: getUserRoleCode(user),
    });
    setActiveSection(SECURITY_TABS.USERS);
  };

  const handleNewUser = () => {
    setCreatingUser(true);
    setSelectedUser(null);
    setUserForm({
      ...emptyUserForm,
      rol: parametricRoles[0]?.itemCodigo || '',
    });
    setActiveSection(SECURITY_TABS.USERS);
  };

  const handleCancelUserEdit = () => {
    setCreatingUser(false);
    setSelectedUser(null);
    setUserForm(emptyUserForm);
  };

  const handleSaveUser = async (event, overrides = {}) => {
    event.preventDefault();

    if (!canConfigure) {
      setError('No tienes permisos para modificar usuarios.');
      return;
    }

    if (!userForm.username.trim()) {
      setError('El username es obligatorio.');
      return;
    }

    try {
      setSavingUser(true);
      setError('');
      await securityService.updateUser({
        username: userForm.username.trim(),
        nombre: userForm.nombre.trim(),
        correo: userForm.correo.trim(),
        dependencia: userForm.dependencia.trim(),
        rol: userForm.rol.trim(),
        activo: typeof overrides.activo === 'boolean' ? overrides.activo : Boolean(userForm.activo),
      });

      setNotice(creatingUser ? 'El usuario se creo correctamente.' : 'El usuario se actualizo correctamente.');
      setCreatingUser(false);
      setSelectedUser(null);
      setUserForm(emptyUserForm);
      await loadData(userSearch);
    } catch (saveError) {
      console.error('Error guardando usuario:', saveError);
      setError('No fue posible guardar el usuario.');
    } finally {
      setSavingUser(false);
    }
  };

  const handleDeactivateUser = async () => {
    await handleSaveUser({ preventDefault: () => { } }, { activo: false });
  };

  const handleSaveAssignment = async (event) => {
    event.preventDefault();

    if (!canConfigure) {
      setError('No tienes permisos para asignar proyectos.');
      return;
    }

    if (!assignmentForm.username || !assignmentForm.proyectoId || !assignmentForm.cargo) {
      setError('Selecciona usuario, proyecto y cargo para continuar.');
      return;
    }

    if (shouldRestrictToDirectors && !isDirectorEquivalentRole(selectedAssignmentUserRoleKey)) {
      setError('El usuario seleccionado no tiene un rol directivo equivalente.');
      return;
    }

    try {
      setSavingAssignment(true);
      setError('');
      await securityService.assignUserToProject({
        username: assignmentForm.username.trim(),
        proyectoId: assignmentForm.proyectoId.trim(),
        cargo: assignmentForm.cargo.trim(),
      });
      setNotice('La asignacion de proyecto se guardo correctamente.');
      await loadAssignments(assignmentForm.username.trim());
      await loadData(userSearch);
    } catch (assignmentSaveError) {
      console.error('Error guardando asignacion:', assignmentSaveError);
      setError('No fue posible guardar la asignacion de proyecto.');
    } finally {
      setSavingAssignment(false);
    }
  };

  const buildMatrixPayload = (extraRoleCode = '', extraPermissions = []) => {
    const payload = Object.fromEntries(
      roles.map((role) => [role.codigo, Array.from(new Set((role.permisos || []).map((permiso) => permiso.codigo)))])
    );

    if (selectedRoleCode && !creatingRole) {
      payload[selectedRoleCode] = Array.from(roleDraftPermissions);
    }

    if (extraRoleCode) {
      payload[extraRoleCode] = extraPermissions;
    }

    return payload;
  };

  const handleSaveRole = async (event) => {
    event.preventDefault();

    if (!canConfigure) {
      setError('No tienes permisos para modificar roles.');
      return;
    }

    if (!roleForm.codigo.trim() || !roleForm.nombre.trim()) {
      setError('El codigo y el nombre del rol son obligatorios.');
      return;
    }

    try {
      setSavingRole(true);
      setSavingPermissions(true);
      setError('');

      const payload = {
        codigo: roleForm.codigo.trim(),
        nombre: roleForm.nombre.trim(),
        descripcion: roleForm.descripcion.trim(),
        transversal: Boolean(roleForm.transversal),
        activo: Boolean(roleForm.activo),
      };

      if (creatingRole) {
        await securityService.createRole(payload);
        await securityService.saveRolePermissions(
          buildMatrixPayload(payload.codigo, Array.from(roleDraftPermissions))
        );
      } else if (selectedRoleCode) {
        await securityService.updateRole(selectedRoleCode, payload);
        await securityService.saveRolePermissions(buildMatrixPayload());
      }

      setNotice(creatingRole ? 'El rol se creo correctamente.' : 'El rol se actualizo correctamente.');
      setCreatingRole(false);
      setRoleEditorOpen(false);
      setRoleTemplateCode('');
      await loadData(userSearch);
      setSelectedRoleCode(payload.codigo);
    } catch (saveError) {
      console.error('Error guardando rol:', saveError);
      setError('No fue posible guardar el rol.');
    } finally {
      setSavingRole(false);
      setSavingPermissions(false);
    }
  };

  const handleSelectRole = (role) => {
    setCreatingRole(false);
    setRoleEditorOpen(true);
    setRoleTemplateCode('');
    setSelectedRoleCode(role.codigo);
    setRoleForm({
      codigo: role.codigo || '',
      nombre: role.nombre || '',
      descripcion: role.descripcion || '',
      transversal: Boolean(role.transversal),
      activo: Boolean(role.activo),
    });
    setRoleDraftPermissions(new Set((role.permisos || []).map((permiso) => permiso.codigo)));
    setActiveSection(SECURITY_TABS.ROLES);
  };

  const handleNewRole = () => {
    setCreatingRole(true);
    setRoleEditorOpen(true);
    setSelectedRoleCode('');
    setRoleForm(emptyRoleForm);
    setRoleTemplateCode(roles[0]?.codigo || '');
    setRoleDraftPermissions(new Set((roles[0]?.permisos || []).map((permiso) => permiso.codigo)));
    setActiveSection(SECURITY_TABS.ROLES);
  };

  const handleRoleTemplateChange = (value) => {
    setRoleTemplateCode(value);
    const template = roles.find((role) => role.codigo === value) || null;
    const templatePermissions = new Set((template?.permisos || []).map((permiso) => permiso.codigo));
    setRoleDraftPermissions(templatePermissions);
  };

  const handleDeleteRole = async () => {
    if (!selectedRoleCode || creatingRole) return;

    if (!canConfigure) {
      setError('No tienes permisos para desactivar roles.');
      return;
    }

    try {
      setSavingRole(true);
      setError('');
      await securityService.deleteRole(selectedRoleCode);
      setNotice('El rol se desactivo correctamente.');
      setCreatingRole(false);
      setRoleEditorOpen(false);
      setRoleTemplateCode('');
      setSelectedRoleCode('');
      setRoleForm(emptyRoleForm);
      setRoleDraftPermissions(new Set());
      await loadData(userSearch);
    } catch (deleteError) {
      console.error('Error desactivando rol:', deleteError);
      setError('No fue posible desactivar el rol.');
    } finally {
      setSavingRole(false);
    }
  };

  const handleCancelRoleEdit = () => {
    setCreatingRole(false);
    setRoleEditorOpen(false);
    setRoleTemplateCode('');

    if (selectedRole) {
      setRoleForm({
        codigo: selectedRole.codigo || '',
        nombre: selectedRole.nombre || '',
        descripcion: selectedRole.descripcion || '',
        transversal: Boolean(selectedRole.transversal),
        activo: Boolean(selectedRole.activo),
      });
      setRoleDraftPermissions(new Set((selectedRole.permisos || []).map((permiso) => permiso.codigo)));
      return;
    }

    setRoleForm(emptyRoleForm);
    setRoleDraftPermissions(new Set());
  };

  const hasRoleEditorOpen = roleEditorOpen;

  const roleTypeLabel = (role) => (role?.transversal ? 'SISTEMA' : 'PERSONALIZADO');
  const roleTypeClass = (role) => (role?.transversal ? 'system' : 'custom');

  const permissionBucketMatches = (permission, bucket) => {
    const action = (permission?.actionKey || '').toUpperCase();
    if (bucket === 'visualizar') {
      return ['VER', 'LISTAR', 'CONSULTAR'].includes(action);
    }
    if (bucket === 'crear') {
      return ['CREAR', 'REGISTRAR', 'CARGAR', 'SUBIR'].includes(action);
    }
    if (bucket === 'editar') {
      return ['EDITAR', 'ACTUALIZAR', 'MODIFICAR'].includes(action);
    }
    return !['VER', 'LISTAR', 'CONSULTAR', 'CREAR', 'REGISTRAR', 'CARGAR', 'SUBIR', 'EDITAR', 'ACTUALIZAR', 'MODIFICAR'].includes(action);
  };

  const matrixColumnDescriptions = {
    visualizar: 'Permite consultar y ver información dentro del módulo.',
    crear: 'Permite registrar o crear nuevos elementos en este módulo.',
    editar: 'Permite modificar registros existentes del módulo.',
    especiales: 'Permisos especiales o acciones transversales definidas para este módulo.',
  };

  const matrixColumnShortcuts = {
    visualizar: 'Consultar y ver registros del módulo.',
    crear: 'Crear o registrar nuevos elementos.',
    editar: 'Modificar elementos ya existentes.',
    especiales: 'Acciones transversales o permisos avanzados.',
  };

  const getBucketCodes = (group, bucket) => group.permissions
    .filter((permission) => permissionBucketMatches(permission, bucket))
    .map((permission) => permission.codigo);

  const isBucketChecked = (codes) => codes.length > 0 && codes.every((code) => roleDraftPermissions.has(code));

  const getMatrixCellTitle = (group, bucket, codes) => {
    const baseDescription = matrixColumnDescriptions[bucket] || bucket;
    if (!codes.length) {
      return `${group.label}: no existen permisos de ${bucket} para este módulo.`;
    }

    const stateDescription = isBucketChecked(codes)
      ? 'Actualmente está activado.'
      : 'Actualmente está desactivado.';
    return `${group.label}. ${baseDescription} ${stateDescription}`;
  };

  const getPermissionTooltip = (permission) => (
    `${permission.nombre}${permission.descripcion ? `: ${permission.descripcion}` : ''}`
  );

  const getConstraintTooltip = (key) => {
    const tooltips = {
      transversal: 'Activa acceso amplio sobre el alcance del rol dentro de los módulos permitidos.',
      activo: 'Define si este rol puede seguir utilizándose en la plataforma.',
      access: 'Acceso heredado desde el contexto del rol. Solo lectura.',
    };

    return tooltips[key] || 'Configuración del comportamiento del rol.';
  };

  const togglePermissionBucket = (codes) => {
    if (!canConfigure || codes.length === 0) return;

    setRoleDraftPermissions((current) => {
      const next = new Set(current);
      const shouldAdd = codes.some((code) => !next.has(code));

      codes.forEach((code) => {
        if (shouldAdd) {
          next.add(code);
        } else {
          next.delete(code);
        }
      });

      return next;
    });
  };

  return (
    <div className="security-admin-page">
      <header className="security-header">
        <div className="security-header-copy">
          <p className="security-eyebrow">Configuracion Seguridad</p>
          <h1>Gestion de Roles y Permisos</h1>
          <p>
            Dos flujos claros, una sola pantalla: usuarios con rol asignable y roles con su matriz integrada en el mismo editor.
          </p>
        </div>

        <div className="security-header-actions">
          <div className="quick-actions" aria-label="Acciones rápidas">
            <button
              type="button"
              className={`quick-action roles ${activeSection === SECURITY_TABS.ROLES ? 'active' : ''}`}
              onClick={() => setActiveSection(SECURITY_TABS.ROLES)}
            >
              <Users size={14} />
              Roles
            </button>
            {canConfigure && (
              <button
                type="button"
                className={`quick-action roles ${activeSection === SECURITY_TABS.ASSIGNMENTS ? 'active' : ''}`}
                onClick={() => {
                  setActiveSection(SECURITY_TABS.ASSIGNMENTS);
                }}
              >
                <Briefcase size={14} />
                Asignaciones
              </button>
            )}
            {canConfigure && (
              <button
                type="button"
                className={`quick-action roles ${activeSection === SECURITY_TABS.NOTIFICATIONS ? 'active' : ''}`}
                onClick={() => setActiveSection(SECURITY_TABS.NOTIFICATIONS)}
              >
                <BellRing size={14} />
                Notificaciones
              </button>
            )}
            {canConfigure && (
              <button
                type="button"
                className={`quick-action roles ${activeSection === SECURITY_TABS.CLOSURE_TEMPLATE ? 'active' : ''}`}
                onClick={() => setActiveSection(SECURITY_TABS.CLOSURE_TEMPLATE)}
              >
                <FileText size={14} />
                Acta de Cierre
              </button>
            )}
            {canConfigure && (
              <button
                type="button"
                className={`quick-action roles ${activeSection === SECURITY_TABS.CLOSURE_QUESTIONS ? 'active' : ''}`}
                onClick={() => setActiveSection(SECURITY_TABS.CLOSURE_QUESTIONS)}
              >
                <ListTodo size={14} />
                Preguntas Cierre
              </button>
            )}
            {canConfigure && (
              <button
                type="button"
                className={`quick-action roles ${activeSection === SECURITY_TABS.LISTAS_PARAMETRICAS ? 'active' : ''}`}
                onClick={() => setActiveSection(SECURITY_TABS.LISTAS_PARAMETRICAS)}
              >
                <List size={14} />
                Listas
              </button>
            )}
            {canConfigure && (
              <button
                type="button"
                className={`quick-action roles ${activeSection === SECURITY_TABS.STORAGE ? 'active' : ''}`}
                onClick={() => setActiveSection(SECURITY_TABS.STORAGE)}
              >
                <FolderOpen size={14} />
                Almacenamiento
              </button>
            )}
            {canConfigure && (
              <button
                type="button"
                className={`quick-action roles ${activeSection === SECURITY_TABS.USER_PERMISSIONS ? 'active' : ''}`}
                onClick={() => setActiveSection(SECURITY_TABS.USER_PERMISSIONS)}
              >
                <ShieldCheck size={14} />
                Permisos
              </button>
            )}
            {canConfigure && (
              <button type="button" className="quick-action create" onClick={handleNewUser}>
                <Plus size={14} />
                Crear Usuario
              </button>
            )}
          </div>
          <button type="button" className="btn-secondary" onClick={handleReload} disabled={loading}>
            <RefreshCw size={16} />
            Actualizar
          </button>
        </div>
      </header>

      {error && (
        <div className="feedback-banner error">
          <CircleAlert size={18} />
          <span>{error}</span>
        </div>
      )}

      {usersLoadError && (
        <div className="feedback-banner error">
          <CircleAlert size={18} />
          <span>{usersLoadError}</span>
        </div>
      )}

      {notice && (
        <div className="feedback-banner success">
          <BadgeCheck size={18} />
          <span>{notice}</span>
        </div>
      )}

      {[SECURITY_TABS.USERS, SECURITY_TABS.ASSIGNMENTS, SECURITY_TABS.NOTIFICATIONS, SECURITY_TABS.CLOSURE_TEMPLATE, SECURITY_TABS.CLOSURE_QUESTIONS, SECURITY_TABS.LISTAS_PARAMETRICAS, SECURITY_TABS.STORAGE, SECURITY_TABS.USER_PERMISSIONS].includes(activeSection) && (
        <section className="security-workspace users-workspace">
          {activeSection === SECURITY_TABS.USERS && (
          <article className="panel panel-main users-panel">
            <div className="panel-topbar">
              <div>
                <h2>Gestión de Usuarios</h2>
                <p>Control de acceso, roles y estados del personal del sistema.</p>
              </div>

              <div className="panel-actions">
                <form className="inline-search" onSubmit={handleSearch}>
                  <Search size={15} />
                  <input
                    type="text"
                    value={userSearch}
                    onChange={(event) => setUserSearch(event.target.value)}
                    placeholder="Buscar usuario"
                  />
                </form>

                <button type="button" className="btn-secondary" onClick={handleReload} disabled={loading}>
                  <RefreshCw size={16} />
                  Refrescar
                </button>
              </div>
            </div>

            {creatingUser && (
              <div className="create-user-panel">
                <div className="create-user-header">
                  <div>
                    <p className="security-eyebrow">Configuración Seguridad</p>
                    <h2>Crear Usuario</h2>
                    <p>Configure los datos básicos y privilegios de acceso al sistema.</p>
                  </div>

                  <button type="button" className="btn-ghost-dark" onClick={handleCancelUserEdit}>
                    <span aria-hidden="true">← </span>
                    Volver al listado
                  </button>
                </div>

                <form className="create-user-form" onSubmit={handleSaveUser}>
                  <div className="create-user-grid">
                    <label className="span-full">
                      <span>Nombre completo *</span>
                      <SpellCheckerInput
                        value={userForm.nombre}
                        onChange={handleUserFieldChange('nombre')}
                        disabled={!canConfigure}
                        placeholder="Ej. Juan Perez"
                      />
                    </label>

                    <label>
                      <span>Usuario (login) *</span>
                      <input
                        value={userForm.username}
                        onChange={handleUserFieldChange('username')}
                        disabled={!canConfigure}
                        placeholder="fasantos"
                      />
                    </label>

                    <label>
                      <span>Rol asignado *</span>
                      <AutocompleteSelect
                        value={userForm.rol}
                        onChange={(val) => handleUserFieldChange('rol')({ target: { value: val } })}
                        disabled={!canConfigure || parametricRoles.length === 0}
                        options={parametricRoles.map((item) => ({
                          value: item.itemCodigo,
                          label: `${item.itemNombre} - ${item.itemCodigo}`,
                        }))}
                        placeholder={parametricRoles.length === 0 ? 'Sin roles disponibles' : '-- Seleccione un rol --'}
                        allLabel=""
                        allValue=""
                      />
                    </label>

                    <label>
                      <span>Correo</span>
                      <input type="email" value={userForm.correo} onChange={handleUserFieldChange('correo')} disabled={!canConfigure} placeholder="correo@dominio.com" />
                    </label>

                    <label>
                      <span>Dependencia</span>
                      <SpellCheckerInput value={userForm.dependencia} onChange={handleUserFieldChange('dependencia')} disabled={!canConfigure} placeholder="Area o dependencia" />
                    </label>

                    <label className="toggle-field compact-toggle">
                      <span>Activo</span>
                      <label className="switch">
                        <input type="checkbox" checked={Boolean(userForm.activo)} onChange={handleUserFieldChange('activo')} disabled={!canConfigure} />
                        <span />
                      </label>
                    </label>
                  </div>

                  <div className="create-user-footer">
                    <button type="button" className="btn-secondary" onClick={handleCancelUserEdit} disabled={savingUser}>
                      Cancelar
                    </button>
                    {canConfigure && (
                      <button type="submit" className="btn-primary" disabled={savingUser}>
                        <BadgeCheck size={16} />
                        {savingUser ? 'Guardando...' : 'Crear Usuario'}
                      </button>
                    )}
                  </div>
                </form>
              </div>
            )}

            {!creatingUser && (
              <div className="table-shell user-table-shell">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Usuario</th>
                      <th>Nombre Completo</th>
                      <th>Rol</th>
                      <th>Estado</th>
                      <th>Último acceso</th>
                      <th>Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {loading ? (
                      <tr>
                        <td colSpan={7} className="table-empty-cell">
                          Cargando usuarios...
                        </td>
                      </tr>
                    ) : usersLoadError ? (
                      <tr>
                        <td colSpan={7} className="table-empty-cell">
                          <div className="empty-state">
                            <strong>Usuarios no disponibles</strong>
                            <span>El backend devolvió un error al consultar la relación proyecta_db.usuarios. Revisa la base de datos o la migración de ese esquema.</span>
                          </div>
                        </td>
                      </tr>
                    ) : filteredUsers.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="table-empty-cell">
                          No hay usuarios disponibles.
                        </td>
                      </tr>
                    ) : (
                      filteredUsers.map((user, index) => {
                        const isSelected = selectedUser?.username === user.username;
                        const initial = (user.nombre || user.username || '?')[0].toUpperCase();
                        const avatarColor = getAvatarColor(user.nombre || user.username);
                        const roleValue = getUserRoleLabel(user) || formatRoleLabel(getUserRoleCode(user)) || 'No verificado';
                        const lastAccess = formatDateTime(getUserLastAccess(user));

                        return (
                          <tr
                            key={user.id || user.username || index}
                            className={isSelected ? 'selected-row' : ''}
                            onClick={() => handleSelectUser(user)}
                          >
                            <td className="id-cell">#{user.id || index + 1}</td>
                            <td>
                              <div className="user-chip">
                                <span className="user-avatar" style={{ background: avatarColor }}>
                                  {initial}
                                </span>
                                <div>
                                  <strong>{user.username}</strong>
                                  <span>@{user.username}</span>
                                </div>
                              </div>
                            </td>
                            <td>{user.nombre || 'Sin nombre'}</td>
                            <td>
                              <span className="soft-pill">{roleValue}</span>
                            </td>
                            <td>
                              <span className={`status-chip ${user.activo ? 'active' : 'inactive'}`}>
                                {user.activo ? 'Activo' : 'Inactivo'}
                              </span>
                            </td>
                            <td>
                              <span className="last-access">
                                <Clock3 size={14} />
                                {lastAccess}
                              </span>
                            </td>
                            <td>
                              {canConfigure ? (
                                <div className="row-actions">
                                  <button
                                    type="button"
                                    className="icon-button"
                                    onClick={(event) => {
                                      event.stopPropagation();
                                      handleSelectUser(user);
                                    }}
                                    title="Editar usuario"
                                  >
                                    <Pencil size={14} />
                                  </button>
                                  <label className="row-toggle" onClick={(event) => event.stopPropagation()}>
                                    <input type="checkbox" checked={Boolean(user.activo)} readOnly />
                                    <span />
                                  </label>
                                </div>
                              ) : null}
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </article>
          )}

          {canConfigure && activeSection === SECURITY_TABS.NOTIFICATIONS && (
            <NotificationTemplatesPanel />
          )}
          {canConfigure && activeSection === SECURITY_TABS.CLOSURE_TEMPLATE && (
            <ClosureTemplatePanel />
          )}
          {canConfigure && activeSection === SECURITY_TABS.CLOSURE_QUESTIONS && (
            <ClosureQuestionsPanel />
          )}
          {canConfigure && activeSection === SECURITY_TABS.LISTAS_PARAMETRICAS && (
            <ListaParametricaPanel />
          )}
          {canConfigure && activeSection === SECURITY_TABS.STORAGE && (
            <StorageConfigPanel />
          )}
          {canConfigure && activeSection === SECURITY_TABS.USER_PERMISSIONS && (
            <UserPermissionMatrix onClose={() => setActiveSection(SECURITY_TABS.USERS)} />
          )}
          {canConfigure && activeSection === SECURITY_TABS.ASSIGNMENTS && (
            <div
              className="assignments-modal-backdrop"
              role="presentation"
              onMouseDown={() => setActiveSection(SECURITY_TABS.USERS)}
            >
              <article
                ref={assignmentsSectionRef}
                className="panel panel-main assignment-panel assignments-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="assignments-modal-title"
                aria-describedby="assignments-modal-description"
                onMouseDown={(event) => event.stopPropagation()}
              >
                <div className="panel-topbar assignments-topbar">
                  <div className="roles-header-copy">
                    <p className="security-eyebrow">ASIGNACIONES DEL SISTEMA</p>
                    <h2 id="assignments-modal-title">Asignación de Proyecto y Cargo</h2>
                    <p>
                      Administra aquí las asignaciones entre usuarios, proyectos y cargos. Este módulo va separado de la gestión
                      de usuarios para mantener el flujo más claro.
                    </p>
                    <p id="assignments-modal-description" className="assignments-modal-description">
                      El formulario y el listado quedan encapsulados en un modal para evitar que el panel crezca sobre la vista principal.
                    </p>
                  </div>

                  <div className="panel-actions assignments-actions">
                    <button type="button" className="modal-close-btn assignments-modal-close" onClick={() => setActiveSection(SECURITY_TABS.USERS)} aria-label="Cerrar modal">
                      <X size={16} />
                    </button>
                    <button type="button" className="btn-secondary" onClick={() => setActiveSection(SECURITY_TABS.USERS)}>
                      <Users size={16} />
                      Volver a Usuarios
                    </button>
                    <button type="button" className="btn-secondary" onClick={handleReload} disabled={loading}>
                      <RefreshCw size={16} />
                      Actualizar
                    </button>
                  </div>
                </div>

                <div className="assignment-header-badges">
                  <span className="soft-pill">{assignmentCargos.length} cargos</span>
                  <span className="soft-pill">{projectOptions.length} proyectos</span>
                </div>

                <div className="assignment-grid">
                  <form className="assignment-form" onSubmit={handleSaveAssignment}>
                    <div className="assignment-form-grid">
                      <label>
                        <span>Usuario *</span>
                        <AutocompleteSelect
                          value={assignmentForm.username}
                          onChange={(val) => handleAssignmentFieldChange('username')({ target: { value: val } })}
                          disabled={!canConfigure || loading || assignableUsers.length === 0}
                          options={assignableUsers.map((user) => ({
                            value: user.username,
                            label: `${user.nombre || user.username} - ${user.username}`,
                          }))}
                          placeholder={assignableUsers.length === 0 ? 'No hay usuarios disponibles' : 'Selecciona un usuario'}
                          allLabel=""
                          allValue=""
                        />
                      </label>

                      <label>
                        <span>Proyecto *</span>
                        <AutocompleteSelect
                          value={assignmentForm.proyectoId}
                          onChange={(val) => handleAssignmentFieldChange('proyectoId')({ target: { value: val } })}
                          disabled={!canConfigure || loading || projectOptions.length === 0}
                          options={projectOptions.map((project) => ({
                            value: getProjectId(project),
                            label: `${getProjectId(project)} - ${getProjectName(project)}`,
                          }))}
                          placeholder={projectOptions.length === 0 ? 'No hay proyectos disponibles' : 'Selecciona un proyecto'}
                          allLabel=""
                          allValue=""
                        />
                      </label>

                      <label className="span-full">
                        <span>Cargo *</span>
                        <AutocompleteSelect
                          value={assignmentForm.cargo}
                          onChange={(val) => handleAssignmentFieldChange('cargo')({ target: { value: val } })}
                          disabled={!canConfigure || loading || assignmentCargos.length === 0}
                          options={assignmentCargos.map((cargo) => ({
                            value: cargo,
                            label: formatAssignmentCargoLabel(cargo),
                          }))}
                          placeholder={assignmentCargos.length === 0 ? 'Sin cargos configurados' : 'Selecciona un cargo'}
                          allLabel=""
                          allValue=""
                        />
                      </label>

                      <div className="assignment-helper span-full">
                        {selectedAssignmentUser ? (
                          <>
                            <strong>{selectedAssignmentUser.nombre || selectedAssignmentUser.username}</strong>
                            <span>
                              {`Rol actual: ${getUserRoleLabel(selectedAssignmentUser) || formatRoleLabel(selectedAssignmentUserRole) || 'No verificado'}.`}
                              {shouldRestrictToDirectors
                                ? ' El cargo de dirección solo se permite para usuarios con un rol directivo equivalente.'
                                : ' Los cargos se validan contra la parametrizaci?n del sistema.'}
                            </span>
                          </>
                        ) : (
                          <>
                            <strong>Selecciona un usuario para comenzar</strong>
                            <span>
                              Si eliges un cargo directivo, el selector limitará los usuarios disponibles a roles equivalentes.
                            </span>
                          </>
                        )}
                      </div>
                    </div>

                    <div className="form-actions assignment-actions">
                      <button
                        type="button"
                        className="btn-secondary"
                        onClick={() => {
                          setAssignmentForm({
                            username: '',
                            proyectoId: '',
                            cargo: '',
                          });
                          setAssignments([]);
                        }}
                        disabled={savingAssignment}
                      >
                        Limpiar
                      </button>
                      <button type="submit" className="btn-primary" disabled={savingAssignment || loading}>
                        <Save size={16} />
                        {savingAssignment ? 'Guardando...' : 'Asignar proyecto'}
                      </button>
                    </div>
                  </form>

                  <div className="assignment-list-panel">
                    <div className="assignment-list-head">
                      <div>
                        <h5>Asignaciones actuales</h5>
                        <p>
                          {assignmentForm.username
                            ? `Mostrando asignaciones de ${selectedAssignmentUser?.nombre || assignmentForm.username}`
                            : 'Selecciona un usuario para ver sus proyectos asignados.'}
                        </p>
                      </div>
                      <button
                        type="button"
                        className="btn-secondary"
                        onClick={() => loadAssignments(assignmentForm.username).catch(console.error)}
                        disabled={!assignmentForm.username || assignmentLoading}
                      >
                        <RefreshCw size={16} />
                        Actualizar
                      </button>
                    </div>

                    <div className="table-shell assignment-table-shell">
                      <table className="data-table assignment-table">
                        <thead>
                          <tr>
                            <th>Proyecto</th>
                            <th>Cargo</th>
                            <th>Estado</th>
                            <th>Asignado</th>
                          </tr>
                        </thead>
                        <tbody>
                          {assignmentLoading ? (
                            <tr>
                              <td colSpan={4} className="table-empty-cell">
                                Cargando asignaciones...
                              </td>
                            </tr>
                          ) : !assignmentForm.username ? (
                            <tr>
                              <td colSpan={4} className="table-empty-cell">
                                Selecciona un usuario para consultar sus asignaciones.
                              </td>
                            </tr>
                          ) : assignments.length === 0 ? (
                            <tr>
                              <td colSpan={4} className="table-empty-cell">
                                <div className="empty-state">
                                  <strong>Sin asignaciones</strong>
                                  <span>Este usuario todav?a no tiene proyectos vinculados.</span>
                                </div>
                              </td>
                            </tr>
                          ) : (
                            assignments.map((assignment) => (
                              <tr key={assignment.id || `${assignment.username}-${assignment.proyectoId}-${assignment.cargo}`}>
                                <td>
                                  <div className="assignment-project-cell">
                                    <strong>{assignment.proyectoNombre || assignment.proyectoId}</strong>
                                    <span>{assignment.proyectoId}</span>
                                  </div>
                                </td>
                                <td>
                                  <span className="soft-pill">{formatAssignmentCargoLabel(assignment.cargo)}</span>
                                </td>
                                <td>
                                  <span className={`status-chip ${assignment.activo ? 'active' : 'inactive'}`}>
                                    {assignment.activo ? 'Activo' : 'Inactivo'}
                                  </span>
                                </td>
                                <td>{formatDateTime(assignment.fechaAsignacion)}</td>
                              </tr>
                            ))
                          )}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </article>
            </div>
          )}

          {isUserEditorOpen && (

            <div
              className="user-modal-backdrop"
              role="presentation"
              onMouseDown={handleCancelUserEdit}
            >
              <article
                className="panel user-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="user-modal-title"
                aria-describedby="user-modal-description"
                onMouseDown={(event) => event.stopPropagation()}
              >
                <form className="editor-form" onSubmit={handleSaveUser}>
                  <div className="editor-head">
                    <div>
                      <h3 id="user-modal-title">Editar Usuario</h3>
                      <p id="user-modal-description">
                        Actualiza la información del usuario y guarda los cambios sin salir de la lista.
                      </p>
                    </div>

                    <div className="editor-badges">
                      <span className="soft-pill">{roles.length} roles</span>
                      {selectedUser && (
                        <span className={`status-chip ${selectedUser.activo ? 'active' : 'inactive'}`}>
                          {selectedUser.activo ? 'Activo' : 'Inactivo'}
                        </span>
                      )}
                      <button type="button" className="modal-close-btn" onClick={handleCancelUserEdit} aria-label="Cerrar modal">
                        <X size={16} />
                      </button>
                    </div>
                  </div>

                  <div className="form-grid">
                    <label>
                      <span>Username</span>
                      <input
                        value={userForm.username}
                        onChange={handleUserFieldChange('username')}
                        disabled={Boolean(selectedUser) && !creatingUser}
                        placeholder="usuario.sistema"
                      />
                    </label>

                    <label>
                      <span>Nombre</span>
                      <SpellCheckerInput value={userForm.nombre} onChange={handleUserFieldChange('nombre')} disabled={!canConfigure} placeholder="Nombre completo" />
                    </label>

                    <label>
                      <span>Correo</span>
                      <input type="email" value={userForm.correo} onChange={handleUserFieldChange('correo')} disabled={!canConfigure} placeholder="correo@dominio.com" />
                    </label>

                    <label>
                      <span>Dependencia</span>
                      <SpellCheckerInput value={userForm.dependencia} onChange={handleUserFieldChange('dependencia')} disabled={!canConfigure} placeholder="Area o dependencia" />
                    </label>

                    <label className="span-full">
                      <span>Rol</span>
                      <AutocompleteSelect
                        value={userForm.rol}
                        onChange={(val) => handleUserFieldChange('rol')({ target: { value: val } })}
                        disabled={!canConfigure || roles.length === 0}
                        options={roles.map((role) => ({
                          value: role.codigo,
                          label: `${role.nombre} - ${role.codigo}`,
                        }))}
                        placeholder={roles.length === 0 ? 'Sin roles disponibles' : 'Selecciona un rol'}
                        allLabel=""
                        allValue=""
                      />
                    </label>

                    <label className="toggle-field">
                      <span>Activo</span>
                      <label className="switch">
                        <input type="checkbox" checked={Boolean(userForm.activo)} onChange={handleUserFieldChange('activo')} disabled={!canConfigure} />
                        <span />
                      </label>
                    </label>
                  </div>

                  <div className="form-actions">
                    <button type="button" className="btn-secondary" onClick={handleCancelUserEdit} disabled={savingUser}>
                      Cancelar
                    </button>
                    {canConfigure && selectedUser && selectedUser.activo && (
                      <button type="button" className="btn-ghost-danger" onClick={handleDeactivateUser} disabled={savingUser}>
                        <Ban size={16} />
                        Desactivar
                      </button>
                    )}
                    {canConfigure && (
                      <button type="submit" className="btn-primary" disabled={savingUser}>
                        <Save size={16} />
                        {savingUser ? 'Guardando...' : 'Guardar usuario'}
                      </button>
                    )}
                  </div>
                </form>
              </article>
            </div>
          )}
        </section>
      )}

      {activeSection === SECURITY_TABS.ROLES && !hasRoleEditorOpen && (
        <section className="security-workspace roles-workspace">
          <article className="panel panel-main roles-panel">
            <div className="panel-topbar roles-topbar">
              <div className="roles-header-copy">
                <p className="security-eyebrow">GESTIÓN DE ROLES</p>
                <h2>Gestión de Roles</h2>
                <p>Administre los niveles de acceso y perfiles de seguridad del sistema.</p>
              </div>

              <div className="panel-actions roles-actions">
                <button type="button" className="btn-secondary" onClick={() => setActiveSection(SECURITY_TABS.USERS)}>
                  <Users size={16} />
                  Usuarios
                </button>
                <div className="inline-search">
                  <Search size={15} />
                  <input
                    type="text"
                    value={roleSearch}
                    onChange={(event) => setRoleSearch(event.target.value)}
                    placeholder="Buscar rol"
                  />
                </div>
                {canConfigure && (
                  <button type="button" className="btn-primary" onClick={handleNewRole} disabled={loading}>
                    <Plus size={16} />
                    Nuevo Rol Personalizado
                  </button>
                )}
              </div>
            </div>

            <div className="table-shell roles-table-shell">
              <table className="data-table roles-table">
                <thead>
                  <tr>
                    <th>Nombre del rol</th>
                    <th>Descripción</th>
                    <th>Tipo de perfil</th>
                    <th>Estado</th>
                    <th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {loading ? (
                    <tr>
                      <td colSpan={5} className="table-empty-cell">Cargando roles...</td>
                    </tr>
                  ) : filteredRoles.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="table-empty-cell">
                        {emptyMessage('No hay roles', 'Crea un rol para empezar a definir su matriz de permisos.')}
                      </td>
                    </tr>
                  ) : (
                    filteredRoles.map((role) => {
                      const selected = role.codigo === selectedRoleCode;
                      return (
                        <tr key={role.codigo} className={selected ? 'selected-row' : ''} onClick={() => handleSelectRole(role)}>
                          <td>
                            <div className="role-table-name">
                              <span className="role-table-badge">
                                <ShieldCheck size={15} />
                              </span>
                              <div>
                                <strong>{role.nombre}</strong>
                                <span>ID: {role.id ? `#${role.id}` : role.codigo}</span>
                              </div>
                            </div>
                          </td>
                          <td className="role-table-description">{role.descripcion || 'Sin descripción'}</td>
                          <td>
                            <span className={`profile-chip ${roleTypeClass(role)}`}>{roleTypeLabel(role)}</span>
                          </td>
                          <td>
                            <span className={`status-chip ${role.activo ? 'active' : 'inactive'}`}>
                              {role.activo ? 'Activo' : 'Inactivo'}
                            </span>
                          </td>
                          <td>
                            {canConfigure ? (
                              <div className="row-actions">
                                <button
                                  type="button"
                                  className="icon-button"
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    handleSelectRole(role);
                                  }}
                                  title="Editar rol"
                                >
                                  <Pencil size={14} />
                                </button>
                                <label className="row-toggle" onClick={(event) => event.stopPropagation()}>
                                  <input type="checkbox" checked={Boolean(role.activo)} readOnly />
                                  <span />
                                </label>
                              </div>
                            ) : null}
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </article>
        </section>
      )}

      {activeSection === SECURITY_TABS.ROLES && hasRoleEditorOpen && (
        <section className="security-workspace roles-workspace">
          <article className="panel panel-aside role-editor premium-role-editor">
            <form className="editor-form role-editor-form" onSubmit={handleSaveRole}>
              <div className="editor-head role-editor-head">
                <div>
                  <div className="role-editor-title-row">
                    <span className="role-editor-icon">
                      <ShieldCheck size={18} />
                    </span>
                    <div>
                      <h3>{creatingRole ? 'Nuevo Rol Personalizado' : 'Editar Perfil de Seguridad'}</h3>
                      <p>
                        {creatingRole
                          ? 'Defina el perfil y ajuste los privilegios antes de guardar.'
                          : `Modificando privilegios para: ${selectedRole?.nombre || 'Rol seleccionado'}`}
                      </p>
                    </div>
                  </div>
                </div>

                <button type="button" className="btn-ghost-dark" onClick={handleCancelRoleEdit}>
                  <span aria-hidden="true">←</span>
                  Volver al Listado
                </button>
              </div>

              <div className="role-core-grid">
                {creatingRole && (
                  <label className="span-full">
                    <span>Código del rol *</span>
                    <input
                      value={roleForm.codigo}
                      onChange={handleRoleFieldChange('codigo')}
                      disabled={!canConfigure}
                      placeholder="ej: visualizadores_maestro"
                    />
                  </label>
                )}

                <label>
                  <span>Nombre del rol *</span>
                  <SpellCheckerInput
                    value={roleForm.nombre}
                    onChange={handleRoleFieldChange('nombre')}
                    disabled={!canConfigure}
                    placeholder="Visualizadores Maestro"
                  />
                </label>

                <label>
                  <span>Descripción del perfil</span>
                  <SpellCheckerTextarea
                    value={roleForm.descripcion}
                    onChange={handleRoleFieldChange('descripcion')}
                    disabled={!canConfigure}
                    placeholder="Pueden ver los estados de las cuentas y revisar índices analíticos"
                    rows={2}
                  />
                </label>
              </div>

              <section className="role-section-card">
                <div className="role-section-header">
                  <div>
                    <h4>Restricciones de Datos y Workflow</h4>
                    <p>Configure el alcance del rol y su comportamiento base dentro de la plataforma.</p>
                  </div>
                </div>

                <div className="role-constraint-grid">
                  <div className="constraint-card">
                    <h5>Visibilidad de Información</h5>
                    <div className="constraint-list">
                      <label className="constraint-item" title={getConstraintTooltip('transversal')}>
                        <input type="checkbox" checked={Boolean(roleForm.transversal)} onChange={handleRoleFieldChange('transversal')} />
                        <span>Privacidad Estricta</span>
                      </label>
                      <label className="constraint-item" title={getConstraintTooltip('activo')}>
                        <input type="checkbox" checked={Boolean(roleForm.activo)} onChange={handleRoleFieldChange('activo')} />
                        <span>Rol Activo</span>
                      </label>
                      <label className="constraint-item" title={getConstraintTooltip('access')}>
                        <input type="checkbox" checked={Boolean(selectedRole?.transversal)} readOnly disabled />
                        <span>Acceso Transversal</span>
                      </label>
                    </div>
                  </div>

                  <div className="constraint-card">
                    <h5>Acceso a Bloques</h5>
                    {creatingRole && (
                      <div className="template-strip template-strip-role">
                        <label>
                          <span>Copiar permisos desde</span>
                          <AutocompleteSelect
                            value={roleTemplateCode}
                            onChange={(val) => handleRoleTemplateChange(val)}
                            disabled={!canConfigure || roles.length === 0}
                            options={roles.map((role) => ({
                              value: role.codigo,
                              label: `${role.nombre} - ${role.codigo}`,
                            }))}
                            placeholder={roles.length === 0 ? 'Sin roles para copiar' : 'Sin plantilla'}
                            allLabel=""
                            allValue=""
                          />
                        </label>
                      </div>
                    )}

                    <div className="workflow-block-grid">
                      {permissionGroups.map((group) => {
                        const codes = getBucketCodes(group, 'visualizar');
                        const checked = isBucketChecked(codes);
                        return (
                          <label
                            key={group.key}
                            className={`workflow-block ${checked ? 'checked' : ''}`}
                            title={getMatrixCellTitle(group, 'visualizar', codes)}
                          >
                            <input
                              type="checkbox"
                              checked={checked}
                              onChange={() => togglePermissionBucket(codes)}
                            />
                            <span className="workflow-block-copy">{group.label}</span>
                          </label>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </section>

              <section className="role-section-card">
                <div className="role-section-header">
                  <div>
                    <h4>Responsabilidades de Procesamiento</h4>
                    <p>Seleccione los módulos donde este rol actuará como operador directo.</p>
                  </div>
                </div>

                <div className="workflow-responsibility-grid">
                  {permissionGroups.map((group) => {
                    const allCodes = group.permissions.map((permission) => permission.codigo);
                    const checked = isBucketChecked(allCodes);
                    return (
                      <label
                        key={group.key}
                        className={`responsibility-card ${checked ? 'checked' : ''}`}
                        title={`${group.label}: habilita permisos de operación directa sobre este módulo.`}
                      >
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => togglePermissionBucket(allCodes)}
                        />
                        <span className="responsibility-copy">{group.label}</span>
                      </label>
                    );
                  })}
                </div>
              </section>

              <section className="role-section-card matrix-section-card">
                <div className="matrix-head">
                  <div>
                    <h4>Matriz de Privilegios del Sistema</h4>
                    <p>Controle qué acciones puede ejecutar este perfil sobre cada módulo funcional.</p>
                  </div>
                  <span className="soft-pill">{permissionGroups.length} módulos</span>
                </div>

                <div className="matrix-table-shell">
                  <table className="matrix-table">
                    <thead>
                      <tr>
                        <th title="Módulo o funcionalidad a la que aplican los permisos.">Módulo / Funcionalidad</th>
                        <th title={`${matrixColumnDescriptions.visualizar} ${matrixColumnShortcuts.visualizar}`}>Visualizar</th>
                        <th title={`${matrixColumnDescriptions.crear} ${matrixColumnShortcuts.crear}`}>Crear</th>
                        <th title={`${matrixColumnDescriptions.editar} ${matrixColumnShortcuts.editar}`}>Editar</th>
                        <th title={`${matrixColumnDescriptions.especiales} ${matrixColumnShortcuts.especiales}`}>Especiales</th>
                      </tr>
                    </thead>
                    <tbody>
                      {permissionGroups.map((group) => {
                        const viewCodes = getBucketCodes(group, 'visualizar');
                        const createCodes = getBucketCodes(group, 'crear');
                        const editCodes = getBucketCodes(group, 'editar');
                        const specialCodes = getBucketCodes(group, 'especiales');
                        const cells = [
                          { key: 'visualizar', codes: viewCodes },
                          { key: 'crear', codes: createCodes },
                          { key: 'editar', codes: editCodes },
                          { key: 'especiales', codes: specialCodes },
                        ];

                        return (
                          <tr key={group.key}>
                            <td>
                              <div
                                className="matrix-module-cell"
                                title={group.permissions.map((permission) => getPermissionTooltip(permission)).join(' · ')}
                              >
                                <span className="matrix-module-icon">
                                  <ShieldCheck size={15} />
                                </span>
                                <div>
                                  <strong>{group.label}</strong>
                                  <span>{group.key}</span>
                                </div>
                              </div>
                            </td>
                            {cells.map((cell) => {
                              const checked = isBucketChecked(cell.codes);
                              const cellTitle = getMatrixCellTitle(group, cell.key, cell.codes);
                              return (
                                <td key={`${group.key}-${cell.key}`} className="matrix-center-cell" title={cellTitle}>
                                  {cell.codes.length === 0 ? (
                                    <span className="muted-text" title={cellTitle}>N/A</span>
                                  ) : (
                                    <label className={`matrix-toggle ${checked ? 'checked' : ''}`} title={cellTitle}>
                                      <input
                                        type="checkbox"
                                        checked={checked}
                                        onChange={() => togglePermissionBucket(cell.codes)}
                                      />
                                      <span />
                                    </label>
                                  )}
                                </td>
                              );
                            })}
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                <div className="form-actions sticky-actions">
                  <button type="button" className="btn-secondary" onClick={handleCancelRoleEdit} disabled={savingRole || savingPermissions}>
                    Cancelar
                  </button>
                  {canConfigure && selectedRole && !creatingRole && (
                    <button type="button" className="btn-ghost-danger" onClick={handleDeleteRole} disabled={savingRole || savingPermissions}>
                      <Ban size={16} />
                      Desactivar
                    </button>
                  )}
                  {canConfigure && (
                    <button type="submit" className="btn-primary" disabled={savingRole || savingPermissions}>
                      <Save size={16} />
                      {savingRole || savingPermissions ? 'Guardando...' : creatingRole ? 'Crear rol' : 'Guardar rol'}
                    </button>
                  )}
                </div>
              </section>
            </form>
          </article>
        </section>
      )}
    </div>
  );
};

export default SecurityConfigPage;

