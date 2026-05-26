import { useEffect, useMemo, useState } from 'react';
import {
  ShieldCheck,
  KeyRound,
  Users,
  FolderKanban,
  Save,
  Search,
  RefreshCw,
  UserPlus,
  BadgeCheck,
  CircleAlert,
  ListChecks,
  Eye,
  AlertTriangle,
} from 'lucide-react';
import { useAuthContext } from '../../context/AuthContext';
import securityService from '../../services/securityService';
import projectService from '../../services/projectService';
import './SecurityConfigPage.css';

const emptyAssignment = {
  username: '',
  proyectoId: '',
  cargo: '',
};

const emptyUserForm = {
  username: '',
  nombre: '',
  correo: '',
  dependencia: '',
  activo: true,
};

const emptyRoleForm = {
  codigo: '',
  nombre: '',
  descripcion: '',
  transversal: false,
  activo: true,
};

const cargoParameterKey = 'seguridad_cargos_asignacion';

const sectionItems = [
  { key: 'usuarios', label: 'Usuarios', description: 'Ver y editar cuentas' },
  { key: 'roles', label: 'Roles', description: 'Administrar los perfiles' },
  { key: 'matriz', label: 'Matriz', description: 'Permisos globales' },
  { key: 'cobertura', label: 'Cobertura', description: 'Pruebas por rol' },
  { key: 'asignaciones', label: 'Asignaciones', description: 'Vincular usuarios' },
];

const coverageModules = [
  { key: 'dashboard', label: 'Dashboard', requiredPermissions: [], note: 'Acceso base de aplicación' },
  { key: 'projects', label: 'Proyectos', requiredPermissions: ['PROYECTO:VER'], note: 'Listado y detalle' },
  { key: 'progress', label: 'Avance del proyecto', requiredPermissions: ['PROYECTO:VER'], note: 'Seguimiento operativo' },
  { key: 'schedule', label: 'Cronograma', requiredPermissions: ['PROYECTO:VER', 'CRONOGRAMA:CARGAR'], note: 'Consulta y carga' },
  { key: 'risk', label: 'Matriz de riesgos', requiredPermissions: ['PROYECTO:VER'], note: 'Sin permiso específico propio' },
  { key: 'closure', label: 'Cierre del proyecto', requiredPermissions: ['PROYECTO:CERRAR'], note: 'Cierre formal' },
  { key: 'reports', label: 'Reportes', requiredPermissions: ['PROYECTO:VER'], note: 'Consulta de analítica' },
  { key: 'security', label: 'Seguridad del sistema', requiredPermissions: ['SISTEMA:CONFIGURAR'], note: 'Administración central' },
];

const coverageFindings = [
  {
    key: 'reportes',
    title: 'Reportes',
    level: 'Pendiente',
    detail: 'No se ve un permiso fino en el controlador de reportes. Conviene protegerlo por funcionalidad.',
  },
  {
    key: 'riesgos',
    title: 'Matriz de riesgos',
    level: 'Parcial',
    detail: 'Tiene acceso base, pero no un permiso dedicado por acción. Puede limitar la segmentacion por rol.',
  },
  {
    key: 'jerarquia',
    title: 'Jerarquia del proyecto',
    level: 'Parcial',
    detail: 'Revisar la exposicion del controlador para asegurar permiso por operacion y no solo por pantalla.',
  },
];

const groupLabel = (groupKey) => {
  const labels = {
    PROYECTO: 'Gestión de Proyectos',
    ENTREGABLE: 'Entregables',
    EVIDENCIA: 'Evidencias',
    DOCUMENTO: 'Documentos',
    CRONOGRAMA: 'Cronograma',
    SISTEMA: 'Administración del Sistema',
    OTROS: 'Otros permisos',
  };

  return labels[groupKey] || groupKey;
};

const parseCargoOptions = (value) => {
  if (!value) {
    return [];
  }

  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
};

const SecurityConfigPage = () => {
  const { permissions: authPermissions, isAdminLocal, transversal, hasRole } = useAuthContext();
  const [roles, setRoles] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [systemParameters, setSystemParameters] = useState([]);
  const [selectedParameterKey, setSelectedParameterKey] = useState(cargoParameterKey);
  const [creatingParameter, setCreatingParameter] = useState(false);
  const [parameterForm, setParameterForm] = useState({
    key: cargoParameterKey,
    value: '',
    descripcion: '',
  });
  const [users, setUsers] = useState([]);
  const [projects, setProjects] = useState([]);
  const [cargoOptions, setCargoOptions] = useState([]);
  const [matrix, setMatrix] = useState({});
  const [selectedUserSearch, setSelectedUserSearch] = useState('');
  const [selectedUser, setSelectedUser] = useState(null);
  const [selectedRoleCode, setSelectedRoleCode] = useState('');
  const [roleTemplateCode, setRoleTemplateCode] = useState('');
  const [roleForm, setRoleForm] = useState(emptyRoleForm);
  const [creatingRole, setCreatingRole] = useState(false);
  const [userForm, setUserForm] = useState(emptyUserForm);
  const [assignment, setAssignment] = useState(emptyAssignment);
  const [assignmentPreview, setAssignmentPreview] = useState([]);
  const [loading, setLoading] = useState(true);
  const [savingRole, setSavingRole] = useState(false);
  const [savingMatrix, setSavingMatrix] = useState(false);
  const [savingUser, setSavingUser] = useState(false);
  const [savingAssignment, setSavingAssignment] = useState(false);
  const [savingParameter, setSavingParameter] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [activeSection, setActiveSection] = useState('usuarios');

  const selectedRole = useMemo(
    () => roles.find((role) => role.codigo === selectedRoleCode) || null,
    [roles, selectedRoleCode]
  );

  const permissionsByGroup = useMemo(() => {
    return permissions.reduce((accumulator, permission) => {
      const groupKey = (permission.codigo || 'OTROS').split(':')[0] || 'OTROS';
      if (!accumulator[groupKey]) {
        accumulator[groupKey] = [];
      }
      accumulator[groupKey].push(permission);
      return accumulator;
    }, {});
  }, [permissions]);

  const cargoParameter = useMemo(
    () => systemParameters.find((parameter) => parameter.key === cargoParameterKey) || null,
    [systemParameters]
  );

  const selectedParameter = useMemo(
    () => systemParameters.find((parameter) => parameter.key === selectedParameterKey) || null,
    [systemParameters, selectedParameterKey]
  );

  useEffect(() => {
    if (selectedRole) {
      setRoleForm({
        codigo: selectedRole.codigo || '',
        nombre: selectedRole.nombre || '',
        descripcion: selectedRole.descripcion || '',
        transversal: Boolean(selectedRole.transversal),
        activo: Boolean(selectedRole.activo),
      });
    }
  }, [selectedRole]);

  useEffect(() => {
    if (creatingParameter) {
      return;
    }

    if (!selectedParameter && systemParameters.length === 0) {
      return;
    }

    const nextParameter = selectedParameter || cargoParameter || systemParameters[0] || null;
    if (!nextParameter) {
      return;
    }

    if (!selectedParameterKey && nextParameter.key && nextParameter.key !== selectedParameterKey) {
      setSelectedParameterKey(nextParameter.key);
    }

    setParameterForm({
      key: nextParameter.key || '',
      value: nextParameter.value || '',
      descripcion: nextParameter.descripcion || '',
    });

    if (nextParameter.key === cargoParameterKey) {
      const parsedCargoOptions = parseCargoOptions(nextParameter.value);
      if (parsedCargoOptions.length > 0) {
        setCargoOptions(parsedCargoOptions);
      }
    }
  }, [creatingParameter, cargoParameter, selectedParameter, selectedParameterKey, systemParameters]);

  useEffect(() => {
    if (selectedParameterKey === cargoParameterKey && !creatingParameter) {
      const parsedCargoOptions = parseCargoOptions(parameterForm.value);
      setCargoOptions(parsedCargoOptions);
      setAssignment((current) => ({
        ...current,
        cargo: parsedCargoOptions.length > 0 ? (current.cargo || parsedCargoOptions[0]) : '',
      }));
    }
  }, [creatingParameter, parameterForm.value, selectedParameterKey]);

  useEffect(() => {
    if (!selectedRoleCode && roles.length > 0 && !creatingRole) {
      setSelectedRoleCode(roles[0].codigo);
    }
  }, [roles, selectedRoleCode, creatingRole]);

  const loadAssignmentsForUser = async (username) => {
    if (!username) {
      setAssignmentPreview([]);
      return;
    }

    try {
      const assignmentsResponse = await securityService.listAssignments(username);
      const assignmentsData = Array.isArray(assignmentsResponse?.data) ? assignmentsResponse.data : [];
      setAssignmentPreview(assignmentsData);
    } catch (assignmentsError) {
      console.error('Error cargando asignaciones del usuario:', assignmentsError);
      setAssignmentPreview([]);
    }
  };

  const loadData = async (search = '') => {
    try {
      setLoading(true);
      setError('');

      let profileData = null;
      try {
        const profileResponse = await securityService.getAuthorization();
        profileData = profileResponse?.data ?? null;
      } catch (profileError) {
        console.warn('No fue posible cargar la autorizacion actual:', profileError);
      }

      const [
        rolesResult,
        permissionsResult,
        usersResult,
        projectsResult,
        cargosResult,
        parametersResult,
      ] = await Promise.allSettled([
        securityService.listRoles({ includeInactive: true }),
        securityService.listPermissions(),
        securityService.listUsers({ search, size: 100 }),
        projectService.getAllUnpaged(),
        securityService.listAssignmentCargos(),
        securityService.listSystemParameters(),
      ]);

      const rolesData = rolesResult.status === 'fulfilled' && Array.isArray(rolesResult.value?.data)
        ? rolesResult.value.data
        : [];
      const permissionsData = permissionsResult.status === 'fulfilled' && Array.isArray(permissionsResult.value?.data)
        ? permissionsResult.value.data
        : [];
      const usersData = usersResult.status === 'fulfilled' && Array.isArray(usersResult.value?.data?.content)
        ? usersResult.value.data.content
        : [];
      const projectsPayload = projectsResult.status === 'fulfilled' && Array.isArray(projectsResult.value)
        ? projectsResult.value
        : [];
      const cargosData = cargosResult.status === 'fulfilled' && Array.isArray(cargosResult.value?.data)
        ? cargosResult.value.data
        : [];
      const parametersData = parametersResult.status === 'fulfilled' && Array.isArray(parametersResult.value?.data)
        ? parametersResult.value.data
        : [];

      setRoles(rolesData);
      setPermissions(permissionsData);
      setSystemParameters(parametersData);
      setUsers(usersData);
      setProjects(projectsPayload);
      const cargoParameterData = parametersData.find((parameter) => parameter.key === cargoParameterKey) || null;
      const derivedCargoOptions = parseCargoOptions(cargoParameterData?.value);
      setCargoOptions(derivedCargoOptions.length > 0 ? derivedCargoOptions : cargosData);
      setAssignment((current) => ({
        ...current,
        cargo: current.cargo || derivedCargoOptions[0] || cargosData[0] || '',
      }));

      const nextMatrix = {};
      rolesData.forEach((role) => {
        nextMatrix[role.codigo] = new Set((role.permisos || []).map((permiso) => permiso.codigo));
      });
      setMatrix(nextMatrix);

      if (selectedRoleCode && !rolesData.some((role) => role.codigo === selectedRoleCode)) {
        setSelectedRoleCode(rolesData[0]?.codigo || '');
      }
      if (!selectedRoleCode && rolesData.length > 0) {
        setSelectedRoleCode(rolesData[0].codigo);
      }

      if (!selectedParameterKey && parametersData.length > 0) {
        setSelectedParameterKey(parametersData[0].key || cargoParameterKey);
      }

      if (selectedUser?.username) {
        await loadAssignmentsForUser(selectedUser.username);
      } else if (profileData?.username) {
        setAssignmentPreview(profileData.proyectosAsignados || []);
      }
    } catch (fetchError) {
      console.error('Error cargando configuracion administrativa:', fetchError);
      setError('No fue posible cargar la configuracion dinamica de seguridad.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData().catch(console.error);
  }, []);

  const handleReload = () => {
    loadData(selectedUserSearch).catch(console.error);
  };

  const handleSearch = async (event) => {
    event.preventDefault();
    await loadData(selectedUserSearch);
  };

  const togglePermission = (roleCode, permissionCode) => {
    setMatrix((current) => {
      const next = { ...current };
      const currentSet = new Set(next[roleCode] || []);
      if (currentSet.has(permissionCode)) {
        currentSet.delete(permissionCode);
      } else {
        currentSet.add(permissionCode);
      }
      next[roleCode] = currentSet;
      return next;
    });
  };

  const handleSaveMatrix = async () => {
    if (!canConfigure) {
      setError('No tienes permisos para modificar la matriz de seguridad.');
      return;
    }

    try {
      setSavingMatrix(true);
      setError('');
      const payload = Object.fromEntries(
        Object.entries(matrix).map(([roleCode, permissionsSet]) => [roleCode, Array.from(permissionsSet)])
      );
      await securityService.saveRolePermissions(payload);
      setNotice('La matriz de permisos se actualizo correctamente.');
      await loadData(selectedUserSearch);
    } catch (saveError) {
      console.error('Error guardando matriz:', saveError);
      setError('No fue posible guardar la matriz de permisos.');
    } finally {
      setSavingMatrix(false);
    }
  };

  const resetRoleForm = () => {
    setCreatingRole(false);
    setRoleForm(emptyRoleForm);
    setSelectedRoleCode('');
  };

  const handleRoleFieldChange = (field) => (event) => {
    const value = field === 'transversal' || field === 'activo' ? event.target.checked : event.target.value;
    setRoleForm((current) => ({ ...current, [field]: value }));
  };

  const handleSelectRole = (role) => {
    setCreatingRole(false);
    setSelectedRoleCode(role.codigo);
    setRoleTemplateCode(role.codigo);
    setRoleForm({
      codigo: role.codigo || '',
      nombre: role.nombre || '',
      descripcion: role.descripcion || '',
      transversal: Boolean(role.transversal),
      activo: Boolean(role.activo),
    });
    setActiveSection('roles');
  };

  const handleNewRole = () => {
    setCreatingRole(true);
    setRoleForm(emptyRoleForm);
    setSelectedRoleCode('');
    setRoleTemplateCode(roles.find((role) => role.codigo === 'director_proyecto')?.codigo || roles[0]?.codigo || '');
    setActiveSection('roles');
  };

  const buildMatrixPayload = (extraRoleCode = '', extraPermissions = []) => {
    const payload = Object.fromEntries(
      Object.entries(matrix).map(([roleCode, permissionsSet]) => [roleCode, Array.from(permissionsSet)])
    );

    if (extraRoleCode) {
      payload[extraRoleCode] = extraPermissions;
    }

    return payload;
  };

  const handleSaveRole = async (event) => {
    event.preventDefault();

    if (!canConfigure) {
      setError('No tienes permisos para administrar roles.');
      return;
    }

    try {
      setSavingRole(true);
      setError('');

      const payload = {
        codigo: roleForm.codigo,
        nombre: roleForm.nombre,
        descripcion: roleForm.descripcion,
        transversal: Boolean(roleForm.transversal),
        activo: Boolean(roleForm.activo),
      };

      if (selectedRoleCode) {
        await securityService.updateRole(selectedRoleCode, payload);
      } else {
        await securityService.createRole(payload);

        const templatePermissions = roleTemplateCode
          ? Array.from(matrix[roleTemplateCode] || [])
          : [];
        const matrixPayload = buildMatrixPayload(roleForm.codigo, templatePermissions);
        await securityService.saveRolePermissions(matrixPayload);
      }

      setNotice('El rol se guardo correctamente.');
      await loadData(selectedUserSearch);
      setCreatingRole(false);
      setSelectedRoleCode(roleForm.codigo || selectedRoleCode);
    } catch (saveError) {
      console.error('Error guardando rol:', saveError);
      setError('No fue posible guardar el rol.');
    } finally {
      setSavingRole(false);
    }
  };

  const handleDeleteRole = async () => {
    if (!selectedRoleCode) {
      return;
    }

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
      resetRoleForm();
      await loadData(selectedUserSearch);
    } catch (deleteError) {
      console.error('Error desactivando rol:', deleteError);
      setError('No fue posible desactivar el rol.');
    } finally {
      setSavingRole(false);
    }
  };

  const handleParameterFieldChange = (field) => (event) => {
    const value = event.target.value;
    setParameterForm((current) => ({ ...current, [field]: value }));
  };

  const handleSelectParameter = (parameter) => {
    setCreatingParameter(false);
    setSelectedParameterKey(parameter.key || cargoParameterKey);
    setParameterForm({
      key: parameter.key || '',
      value: parameter.value || '',
      descripcion: parameter.descripcion || '',
    });
    setActiveSection('roles');
  };

  const handleNewParameter = () => {
    setCreatingParameter(true);
    setSelectedParameterKey('');
    setParameterForm({
      key: '',
      value: '',
      descripcion: '',
    });
    setActiveSection('roles');
  };

  const handleSaveParameter = async () => {
    if (!canConfigure) {
      setError('No tienes permisos para administrar parametros.');
      return;
    }

    const normalizedKey = (parameterForm.key || '').trim().toLowerCase();
    const normalizedValue = (parameterForm.value || '').trim();
    if (!normalizedKey || !normalizedValue) {
      setError('La clave y el valor del parametro son obligatorios.');
      return;
    }

    try {
      setSavingParameter(true);
      setError('');
      await securityService.saveSystemParameter({
        key: normalizedKey,
        value: normalizedValue,
        descripcion: (parameterForm.descripcion || '').trim(),
      });
      setNotice('El parametro se actualizo correctamente.');
      setCreatingParameter(false);
      await loadData(selectedUserSearch);
    } catch (parameterError) {
      console.error('Error guardando parametro del sistema:', parameterError);
      setError('No fue posible guardar el parametro del sistema.');
    } finally {
      setSavingParameter(false);
    }
  };

  const handleDeleteParameter = async () => {
    if (!canConfigure || !selectedParameterKey) {
      return;
    }

    if (selectedParameterKey === cargoParameterKey) {
      setError('No se puede eliminar el parametro de cargos de asignacion porque es requerido por el sistema.');
      return;
    }

    try {
      setSavingParameter(true);
      setError('');
      await securityService.deleteSystemParameter(selectedParameterKey);
      setNotice('El parametro se elimino correctamente.');
      setSelectedParameterKey(cargoParameterKey);
      setCreatingParameter(false);
      await loadData(selectedUserSearch);
    } catch (parameterError) {
      console.error('Error eliminando parametro del sistema:', parameterError);
      setError('No fue posible eliminar el parametro del sistema.');
    } finally {
      setSavingParameter(false);
    }
  };

  const handleUserFieldChange = (field) => (event) => {
    const value = field === 'activo' ? event.target.checked : event.target.value;
    setUserForm((current) => ({ ...current, [field]: value }));
  };

  const handleEditUser = async (user) => {
    setSelectedUser(user);
    setUserForm({
      username: user.username || '',
      nombre: user.nombre || '',
      correo: user.correo || '',
      dependencia: user.dependencia || '',
      activo: Boolean(user.activo),
    });
    setActiveSection('usuarios');
    await loadAssignmentsForUser(user.username);
  };

  const handleCancelUserEdit = () => {
    setSelectedUser(null);
    setUserForm(emptyUserForm);
    loadData(selectedUserSearch).catch(console.error);
  };

  const handleSaveUser = async (event) => {
    event.preventDefault();

    if (!canConfigure) {
      setError('No tienes permisos para editar usuarios.');
      return;
    }

    try {
      setSavingUser(true);
      setError('');

      const response = await securityService.updateUser({
        username: userForm.username,
        nombre: userForm.nombre,
        correo: userForm.correo,
        dependencia: userForm.dependencia,
        activo: userForm.activo,
      });

      const updatedUser = response?.data ?? null;
      setNotice('El usuario fue actualizado correctamente.');
      if (updatedUser) {
        setSelectedUser(updatedUser);
        setUserForm({
          username: updatedUser.username || userForm.username,
          nombre: updatedUser.nombre || userForm.nombre,
          correo: updatedUser.correo || userForm.correo,
          dependencia: updatedUser.dependencia || userForm.dependencia,
          activo: Boolean(updatedUser.activo),
        });
      }

      await loadData(selectedUserSearch);
      await loadAssignmentsForUser(userForm.username);
    } catch (saveError) {
      console.error('Error actualizando usuario:', saveError);
      setError('No fue posible actualizar el usuario.');
    } finally {
      setSavingUser(false);
    }
  };

  const handleAssignChange = (field) => (event) => {
    const value = event.target.value;
    setAssignment((current) => ({ ...current, [field]: value }));
  };

  const handleAssignUser = async (event) => {
    event.preventDefault();

    if (!canConfigure) {
      setError('No tienes permisos para asignar usuarios a proyectos.');
      return;
    }

    try {
      setSavingAssignment(true);
      setError('');
      await securityService.assignUserToProject(assignment);
      setNotice('El usuario fue asignado al proyecto correctamente.');
      setAssignment({
        ...emptyAssignment,
        cargo: cargoOptions[0] || '',
      });
      await loadData(selectedUserSearch);
    } catch (assignError) {
      console.error('Error asignando usuario a proyecto:', assignError);
      setError('No fue posible guardar la asignacion del usuario al proyecto.');
    } finally {
      setSavingAssignment(false);
    }
  };

  const filteredUsers = users.filter((item) => {
    const haystack = `${item.nombre || ''} ${item.correo || ''} ${item.username || ''}`.toLowerCase();
    return haystack.includes(selectedUserSearch.toLowerCase());
  });

  const rolesCount = roles.length;
  const permissionsCount = permissions.length;
  const canConfigure = isAdminLocal || transversal || hasRole('ADMIN') || hasRole('GESTOR_TIC') || authPermissions.includes('SISTEMA:CONFIGURAR');
  const assignedRoleCoverage = useMemo(() => {
    return roles.map((role) => {
      const permissionsSet = matrix[role.codigo] || new Set();
      const totalApplicable = coverageModules.filter((module) => module.requiredPermissions.length > 0).length;
      const complete = coverageModules.filter((module) => {
        if (module.requiredPermissions.length === 0) {
          return true;
        }
        return module.requiredPermissions.every((permissionCode) => permissionsSet.has(permissionCode));
      }).length;
      const partial = coverageModules.filter((module) => {
        if (module.requiredPermissions.length === 0) {
          return false;
        }
        const matched = module.requiredPermissions.filter((permissionCode) => permissionsSet.has(permissionCode)).length;
        return matched > 0 && matched < module.requiredPermissions.length;
      }).length;
      const pending = Math.max(totalApplicable - complete - partial, 0);

      return {
        role,
        complete,
        partial,
        pending,
      };
    });
  }, [matrix, roles]);

  const getCoverageState = (roleCode, module) => {
    if (module.requiredPermissions.length === 0) {
      return { label: 'Base', tone: 'base' };
    }

    const rolePermissions = matrix[roleCode] || new Set();
    const matchedCount = module.requiredPermissions.filter((permissionCode) => rolePermissions.has(permissionCode)).length;

    if (matchedCount === module.requiredPermissions.length) {
      return { label: 'OK', tone: 'ok' };
    }

    if (matchedCount > 0) {
      return { label: 'Parcial', tone: 'partial' };
    }

    return { label: 'Pendiente', tone: 'pending' };
  };

  const coverageStats = useMemo(() => {
    const totalModules = coverageModules.length;
    const totalRoles = roles.length;
    const rolesWithFullCoverage = assignedRoleCoverage.filter((item) => item.pending === 0 && item.partial === 0 && item.complete > 0).length;
    const criticalFindings = coverageFindings.filter((item) => item.level === 'Pendiente').length;

    return {
      totalModules,
      totalRoles,
      rolesWithFullCoverage,
      criticalFindings,
    };
  }, [assignedRoleCoverage, roles]);

  const rolePermissionCount = (roleCode) => {
    return matrix[roleCode] ? Array.from(matrix[roleCode]).length : 0;
  };

  const assignmentCargoValue = assignment.cargo || cargoOptions[0] || '';

  const renderPermissionGroups = () => {
    if (!selectedRole) {
      return <div className="empty-state">No hay roles disponibles para editar.</div>;
    }

    return (
      <>
        <div className="role-editor-header">
          <div>
            <h3>{selectedRole.nombre}</h3>
            <p>{selectedRole.descripcion || 'Sin descripcion'}</p>
          </div>
          <div className="role-summary-badges">
            <span className="summary-badge">{selectedRole.codigo}</span>
            <span className="summary-badge">{selectedRole.transversal ? 'Transversal' : 'Negocio'}</span>
            <span className={`summary-badge ${selectedRole.activo ? 'good' : 'bad'}`}>
              {selectedRole.activo ? 'Activo' : 'Inactivo'}
            </span>
          </div>
        </div>

        <div className="role-editor-note">
          Selecciona los permisos funcionales que este rol tendra disponibles en el sistema.
        </div>

        <div className="permission-groups">
          {Object.entries(permissionsByGroup).map(([groupKey, items]) => (
            <div key={groupKey} className="permission-group">
              <div className="permission-group-header">
                <strong>{groupLabel(groupKey)}</strong>
                <span>{items.length} permisos</span>
              </div>
              <div className="permission-grid">
                {items.map((permission) => {
                  const checked = Boolean(matrix[selectedRole.codigo]?.has(permission.codigo));
                  return (
                    <label key={permission.codigo} className="permission-chip">
                      <input
                        type="checkbox"
                        checked={checked}
                        disabled={!canConfigure}
                        onChange={() => togglePermission(selectedRole.codigo, permission.codigo)}
                      />
                      <span className="permission-chip-box" />
                      <div className="permission-chip-text">
                        <strong>{permission.nombre}</strong>
                        <small>{permission.codigo}</small>
                      </div>
                    </label>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </>
    );
  };

  return (
    <div className="security-admin-page">
      <header className="security-page-topbar">
        <div className="page-title-block">
          <h1>Gestión de Roles del Sistema</h1>
          <p>Administra los perfiles y ajusta permisos atómicos desde una vista tipo editor.</p>
        </div>
        <div className="page-topbar-actions">
          <span className="page-topbar-pill">{isAdminLocal ? 'admin' : 'usuario'}</span>
          <button type="button" className="btn-secondary" onClick={handleReload} disabled={loading}>
            <RefreshCw size={16} />
            Refrescar
          </button>
        </div>
      </header>

      <section className="security-tabs" aria-label="Secciones de seguridad">
        {sectionItems.map((section) => (
          <button
            key={section.key}
            type="button"
            className={`security-tab ${activeSection === section.key ? 'active' : ''}`}
            onClick={() => setActiveSection(section.key)}
          >
            <strong>{section.label}</strong>
            <span>{section.description}</span>
          </button>
        ))}
      </section>

      <section className="stats-grid">
        <article className="stat-card">
          <Users size={20} />
          <div>
            <span>Usuarios registrados</span>
            <strong>{users.length}</strong>
          </div>
        </article>
        <article className="stat-card">
          <BadgeCheck size={20} />
          <div>
            <span>Roles configurados</span>
            <strong>{rolesCount}</strong>
          </div>
        </article>
        <article className="stat-card">
          <FolderKanban size={20} />
          <div>
            <span>Proyectos</span>
            <strong>{projects.length}</strong>
          </div>
        </article>
        <article className="stat-card">
          <ListChecks size={20} />
          <div>
            <span>Permisos atomicos</span>
            <strong>{permissionsCount}</strong>
          </div>
        </article>
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

      {activeSection === 'roles' && (
        <section className="config-panel">
          <div className="section-title">
            <div>
              <h2>Gestión de Roles del Sistema</h2>
              <p>Administra los perfiles y ajusta permisos atomicos desde una vista tipo editor.</p>
            </div>
            <div className="section-actions">
              <span className="status-pill">{selectedRole ? selectedRole.codigo : 'Sin rol'}</span>
              <button type="button" className="btn-secondary" onClick={handleNewRole} disabled={!canConfigure || loading}>
                Nuevo rol
              </button>
              <button type="button" className="btn-primary" onClick={handleSaveRole} disabled={savingRole || loading || !canConfigure}>
                <Save size={16} />
                {savingRole ? 'Guardando...' : selectedRoleCode ? 'Guardar rol' : 'Crear rol'}
              </button>
              <button type="button" className="btn-secondary" onClick={handleSaveMatrix} disabled={savingMatrix || loading || !canConfigure}>
                <Save size={16} />
                {savingMatrix ? 'Guardando...' : 'Guardar permisos'}
              </button>
              {selectedRoleCode && (
                <button type="button" className="btn-secondary" onClick={handleDeleteRole} disabled={savingRole || loading || !canConfigure}>
                  Desactivar
                </button>
              )}
            </div>
          </div>

          <form className="role-form-panel" onSubmit={handleSaveRole}>
            <div className="section-title compact">
              <div>
                <h3>{selectedRoleCode ? 'Editar rol' : 'Nuevo rol'}</h3>
                <p>Los roles protegidos no pueden eliminarse ni apagarse desde la consola.</p>
              </div>
            </div>

            <div className="editor-grid role-form-grid">
              <label>
                <span>Codigo del rol</span>
                <input
                  value={roleForm.codigo}
                  onChange={handleRoleFieldChange('codigo')}
                  disabled={Boolean(selectedRoleCode) || !canConfigure}
                  placeholder="ej: coordinador_calidad"
                />
              </label>
              <label>
                <span>Nombre</span>
                <input value={roleForm.nombre} onChange={handleRoleFieldChange('nombre')} disabled={!canConfigure} />
              </label>
              <label className="role-form-wide">
                <span>Descripcion</span>
                <input value={roleForm.descripcion} onChange={handleRoleFieldChange('descripcion')} disabled={!canConfigure} />
              </label>
              <label className="switch-field">
                <span>Transversal</span>
                <label className="switch">
                  <input type="checkbox" checked={Boolean(roleForm.transversal)} onChange={handleRoleFieldChange('transversal')} disabled={!canConfigure} />
                  <span />
                </label>
              </label>
                <label className="switch-field">
                  <span>Activo</span>
                  <label className="switch">
                    <input type="checkbox" checked={Boolean(roleForm.activo)} onChange={handleRoleFieldChange('activo')} disabled={!canConfigure} />
                    <span />
                  </label>
                </label>
                {creatingRole && (
                  <label className="role-form-wide">
                    <span>Copiar permisos desde</span>
                    <select value={roleTemplateCode} onChange={(event) => setRoleTemplateCode(event.target.value)} disabled={!canConfigure || roles.length === 0}>
                      <option value="">Sin plantilla</option>
                      {roles.map((role) => (
                        <option key={role.codigo} value={role.codigo}>
                          {role.nombre} - {role.codigo}
                        </option>
                      ))}
                    </select>
                  </label>
                )}
              </div>
            </form>

          {loading ? (
            <div className="empty-state">Cargando configuracion...</div>
          ) : (
            <>
            <div className="role-workspace">
              <aside className="role-catalog">
                <div className="role-catalog-header">
                  <strong>Perfiles de Usuario Configurados</strong>
                  <span>{roles.length} roles</span>
                </div>

                {roles.map((role) => (
                  <button
                    type="button"
                    key={role.codigo}
                    className={`role-card ${selectedRoleCode === role.codigo ? 'active' : ''}`}
                    onClick={() => handleSelectRole(role)}
                  >
                    <div className="role-card-icon">
                      <ShieldCheck size={18} />
                    </div>
                    <div className="role-card-body">
                      <strong>{role.nombre}</strong>
                      <span>{role.descripcion || 'Sin descripcion'}</span>
                      <div className="role-card-meta">
                        <span>{role.codigo}</span>
                        <span>{rolePermissionCount(role.codigo)} permisos</span>
                      </div>
                    </div>
                    <div className={`role-card-pill ${role.transversal ? 'transversal' : 'business'}`}>
                      {role.transversal ? 'TRANSVERSAL' : 'NEGOCIO'}
                    </div>
                  </button>
                ))}
              </aside>

              <article className="role-editor-panel">
                {renderPermissionGroups()}
              </article>
            </div>

            <div className="parameter-panel">
              <div className="section-title compact">
                <div>
                  <h3>Parametros del sistema</h3>
                  <p>Todos los parametros editables se guardan en base de datos y dejan de depender del frontend.</p>
                </div>
                <div className="section-actions">
                  <button type="button" className="btn-secondary" onClick={handleNewParameter} disabled={savingParameter || !canConfigure}>
                    Nuevo parametro
                  </button>
                  <button type="button" className="btn-secondary" onClick={handleDeleteParameter} disabled={savingParameter || !canConfigure || !selectedParameterKey}>
                    Eliminar
                  </button>
                  <button type="button" className="btn-primary" onClick={handleSaveParameter} disabled={savingParameter || !canConfigure}>
                    <Save size={16} />
                    {savingParameter ? 'Guardando...' : 'Guardar parametro'}
                  </button>
                </div>
              </div>

              <div className="parameter-list parameter-list-selectable">
                {systemParameters.map((parameter) => (
                  <button
                    type="button"
                    key={parameter.key}
                    className={`parameter-row parameter-row-button ${selectedParameterKey === parameter.key ? 'selected' : ''}`}
                    onClick={() => handleSelectParameter(parameter)}
                  >
                    <div>
                      <strong>{parameter.key}</strong>
                      <span>{parameter.descripcion || 'Sin descripcion'}</span>
                    </div>
                    <code>{parameter.value}</code>
                  </button>
                ))}
              </div>

              <div className="parameter-card">
                <div className="parameter-card-head">
                  <strong>{parameterForm.key || cargoParameterKey}</strong>
                  <span>{creatingParameter ? 'Nuevo parametro' : selectedParameterKey === cargoParameterKey ? `${cargoOptions.length} cargos` : 'Parametro editable'}</span>
                </div>
                <div className="parameter-form-grid">
                  <label>
                    <span>Clave</span>
                    <input
                      value={parameterForm.key}
                      onChange={handleParameterFieldChange('key')}
                      disabled={!canConfigure || (selectedParameterKey && !creatingParameter)}
                      placeholder="clave_del_parametro"
                    />
                  </label>
                  <label>
                    <span>Valor</span>
                    <textarea
                      rows={4}
                      value={parameterForm.value}
                      onChange={handleParameterFieldChange('value')}
                      disabled={!canConfigure}
                      placeholder="Valor del parametro"
                    />
                  </label>
                  <label>
                    <span>Descripcion</span>
                    <textarea
                      rows={2}
                      value={parameterForm.descripcion}
                      onChange={handleParameterFieldChange('descripcion')}
                      disabled={!canConfigure}
                      placeholder="Descripcion funcional del parametro"
                    />
                  </label>
                </div>
                {selectedParameterKey === cargoParameterKey && !creatingParameter && (
                  <p className="parameter-help">
                    Los cargos se leen desde este parametro y se usan en la asignacion de usuarios a proyectos.
                  </p>
                )}
              </div>
            </div>
            </>
          )}
        </section>
      )}

      {activeSection === 'cobertura' && (
        <section className="config-panel">
          <div className="section-title">
            <div>
              <h2>Cobertura global por rol</h2>
              <p>Vista de validacion para probar diferentes usuarios, revisar permisos y detectar brechas del sistema.</p>
            </div>
            <div className="section-actions">
              <span className="status-pill">{coverageStats.totalRoles} roles</span>
              <span className="status-pill">{coverageStats.totalModules} modulos</span>
            </div>
          </div>

          <section className="coverage-summary-grid">
            <article className="coverage-summary-card">
              <Eye size={18} />
              <div>
                <span>Roles revisados</span>
                <strong>{coverageStats.totalRoles}</strong>
              </div>
            </article>
            <article className="coverage-summary-card">
              <BadgeCheck size={18} />
              <div>
                <span>Roles con cobertura completa</span>
                <strong>{coverageStats.rolesWithFullCoverage}</strong>
              </div>
            </article>
            <article className="coverage-summary-card">
              <AlertTriangle size={18} />
              <div>
                <span>Brechas criticas</span>
                <strong>{coverageStats.criticalFindings}</strong>
              </div>
            </article>
          </section>

          <div className="coverage-layout">
            <article className="coverage-panel">
              <div className="section-title compact">
                <div>
                  <h3>Matriz de cobertura por modulo</h3>
                  <p>Compara la cobertura de acceso entre roles para cada funcionalidad principal.</p>
                </div>
              </div>

              <div className="coverage-table-wrap">
                <table className="coverage-table">
                  <thead>
                    <tr>
                      <th>Modulo</th>
                      {roles.map((role) => (
                        <th key={role.codigo}>
                          <div className="role-th">
                            <strong>{role.nombre}</strong>
                            <span>{role.codigo}</span>
                          </div>
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {coverageModules.map((module) => (
                      <tr key={module.key}>
                        <td>
                          <div className="permission-cell">
                            <strong>{module.label}</strong>
                            <span>{module.note}</span>
                          </div>
                        </td>
                        {roles.map((role) => {
                          const coverageState = getCoverageState(role.codigo, module);
                          return (
                            <td key={`${role.codigo}-${module.key}`} className="matrix-checkbox-cell">
                              <span className={`coverage-badge ${coverageState.tone}`}>{coverageState.label}</span>
                            </td>
                          );
                        })}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </article>

            <aside className="coverage-panel">
              <div className="section-title compact">
                <div>
                  <h3>Brechas detectadas</h3>
                  <p>Puntos a revisar para pruebas de permisos y endurecimiento del sistema.</p>
                </div>
              </div>

              <div className="coverage-findings">
                {coverageFindings.map((finding) => (
                  <article key={finding.key} className={`coverage-finding ${finding.level.toLowerCase()}`}>
                    <div className="coverage-finding-head">
                      <strong>{finding.title}</strong>
                      <span>{finding.level}</span>
                    </div>
                    <p>{finding.detail}</p>
                  </article>
                ))}
              </div>

              <div className="coverage-role-list">
                <div className="coverage-role-list-head">
                  <strong>Resumen por rol</strong>
                  <span>OK / Parcial / Pendiente</span>
                </div>
                {assignedRoleCoverage.map((item) => (
                  <div key={item.role.codigo} className="coverage-role-row">
                    <div>
                      <strong>{item.role.nombre}</strong>
                      <span>{item.role.codigo}</span>
                    </div>
                    <div className="coverage-role-metrics">
                      <span className="coverage-badge ok">{item.complete} OK</span>
                      <span className="coverage-badge partial">{item.partial} Parcial</span>
                      <span className="coverage-badge pending">{item.pending} Pend.</span>
                    </div>
                  </div>
                ))}
              </div>
            </aside>
          </div>
        </section>
      )}

      {activeSection === 'matriz' && (
        <section className="config-panel">
          <div className="section-title">
            <div>
              <h2>Matriz de Privilegios del Sistema</h2>
              <p>Vista completa para revisar que puede hacer cada rol. Los cambios se guardan desde la pestaña Roles.</p>
            </div>
            <div className="section-actions">
              <span className="status-pill">{permissionsCount} permisos</span>
              <button type="button" className="btn-secondary" onClick={handleSaveMatrix} disabled={savingMatrix || loading || !canConfigure}>
                <Save size={16} />
                {savingMatrix ? 'Guardando...' : 'Guardar matriz'}
              </button>
            </div>
          </div>

          {loading ? (
            <div className="empty-state">Cargando matriz...</div>
          ) : (
            <div className="matrix-wrapper matrix-compact">
              <table className="matrix-table">
                <thead>
                  <tr>
                    <th>Modulo / Funcionalidad</th>
                    {roles.map((role) => (
                      <th key={role.codigo}>
                        <div className="role-th">
                          <strong>{role.nombre}</strong>
                          <span>{role.codigo}</span>
                        </div>
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {permissions.map((permission) => (
                    <tr key={permission.codigo}>
                      <td>
                        <div className="permission-cell">
                          <strong>{permission.codigo}</strong>
                          <span>{permission.nombre}</span>
                        </div>
                      </td>
                      {roles.map((role) => {
                        const checked = Boolean(matrix[role.codigo]?.has(permission.codigo));
                        return (
                          <td key={`${role.codigo}-${permission.codigo}`} className="matrix-checkbox-cell">
                            <label className="checkbox-wrap">
                              <input
                                type="checkbox"
                                checked={checked}
                                disabled={!canConfigure}
                                onChange={() => togglePermission(role.codigo, permission.codigo)}
                              />
                              <span />
                            </label>
                          </td>
                        );
                      })}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      {activeSection === 'usuarios' && (
        <section className="config-panel">
          <div className="section-title">
            <div>
              <h2>Usuarios del sistema</h2>
              <p>Selecciona un usuario para editar sus datos base y revisar sus asignaciones.</p>
            </div>
            <span className="status-pill">{selectedUser ? `Editando: ${selectedUser.username}` : 'Modo consulta'}</span>
          </div>

          <form className="search-form" onSubmit={handleSearch}>
            <Search size={16} />
            <input
              type="text"
              value={selectedUserSearch}
              onChange={(event) => setSelectedUserSearch(event.target.value)}
              placeholder="Buscar usuario por nombre, correo o username"
            />
            <button type="submit" className="btn-secondary compact">
              Buscar
            </button>
          </form>

          <div className="users-management">
            <div className="users-table-card">
              <div className="users-table">
                <div className="users-table-head">
                  <span>Usuario</span>
                  <span>Correo</span>
                  <span>Dependencia</span>
                  <span>Estado</span>
                  <span />
                </div>
                {loading ? (
                  <div className="empty-state">Cargando usuarios...</div>
                ) : filteredUsers.length === 0 ? (
                  <div className="empty-state">No hay usuarios disponibles.</div>
                ) : (
                  filteredUsers.map((item) => (
                    <button
                      key={item.id}
                      type="button"
                      className={`user-row ${selectedUser?.username === item.username ? 'selected' : ''}`}
                      onClick={() => handleEditUser(item)}
                    >
                      <span className="user-row-main">
                        <strong>{item.nombre}</strong>
                        <small>{item.username}</small>
                      </span>
                      <span>{item.correo}</span>
                      <span>{item.dependencia || 'No definida'}</span>
                      <span className={`user-status ${item.activo ? 'active' : 'inactive'}`}>
                        {item.activo ? 'Activo' : 'Inactivo'}
                      </span>
                      <span className="user-row-action">Editar</span>
                    </button>
                  ))
                )}
              </div>
            </div>

            <form className="user-editor-card" onSubmit={handleSaveUser}>
              <div className="section-title compact">
                <div>
                  <h3>Edicion de usuario</h3>
                  <p>Actualiza nombre, correo, dependencia y estado.</p>
                </div>
              </div>

              <div className="editor-grid">
                <label>
                  <span>Username</span>
                  <input value={userForm.username} onChange={handleUserFieldChange('username')} disabled />
                </label>
                <label>
                  <span>Nombre</span>
                  <input value={userForm.nombre} onChange={handleUserFieldChange('nombre')} disabled={!canConfigure} />
                </label>
                <label>
                  <span>Correo</span>
                  <input type="email" value={userForm.correo} onChange={handleUserFieldChange('correo')} disabled={!canConfigure} />
                </label>
                <label>
                  <span>Dependencia</span>
                  <input value={userForm.dependencia} onChange={handleUserFieldChange('dependencia')} disabled={!canConfigure} />
                </label>
                <label className="switch-field">
                  <span>Activo</span>
                  <label className="switch">
                    <input type="checkbox" checked={Boolean(userForm.activo)} onChange={handleUserFieldChange('activo')} disabled={!canConfigure} />
                    <span />
                  </label>
                </label>
              </div>

              <div className="editor-actions">
                <button type="button" className="btn-secondary" onClick={handleCancelUserEdit} disabled={savingUser}>
                  Cancelar
                </button>
                <button type="submit" className="btn-primary" disabled={savingUser || !userForm.username || !canConfigure}>
                  <Save size={16} />
                  {savingUser ? 'Guardando...' : 'Guardar usuario'}
                </button>
              </div>
            </form>
          </div>
        </section>
      )}

      {activeSection === 'asignaciones' && (
        <section className="config-columns">
          <article className="config-card">
            <div className="section-title compact">
              <div>
                <h2>Asignacion de proyectos a usuarios</h2>
                <p>Busca un usuario y vinculalo a un proyecto con un cargo especifico.</p>
              </div>
            </div>

            <div className="assignment-panel-note">
              <strong>Usuario seleccionado:</strong>
              <span>{selectedUser ? `${selectedUser.nombre} (${selectedUser.username})` : 'Ninguno'}</span>
            </div>

            <div className="assignment-grid">
              <label>
                <span>Usuario</span>
                <select value={assignment.username} onChange={handleAssignChange('username')} disabled={!canConfigure}>
                  <option value="">Seleccionar usuario</option>
                  {filteredUsers.map((item) => (
                    <option key={item.id} value={item.username}>
                      {item.nombre} - {item.correo}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                <span>Proyecto</span>
                <select value={assignment.proyectoId} onChange={handleAssignChange('proyectoId')} disabled={!canConfigure}>
                  <option value="">Seleccionar proyecto</option>
                  {projects.map((project) => (
                    <option key={project.id} value={project.id}>
                      {project.codigo || project.id} - {project.nombre}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                <span>Cargo en el proyecto</span>
                <select
                  value={assignmentCargoValue}
                  onChange={handleAssignChange('cargo')}
                  disabled={!canConfigure || cargoOptions.length === 0}
                >
                  {cargoOptions.length === 0 ? (
                    <option value="">Sin cargos configurados</option>
                  ) : (
                    cargoOptions.map((cargo) => (
                      <option key={cargo} value={cargo}>
                        {cargo}
                      </option>
                    ))
                  )}
                </select>
              </label>
            </div>

            <div className="editor-actions">
              <button
                type="button"
                className="btn-primary"
                onClick={handleAssignUser}
                disabled={savingAssignment || !canConfigure || cargoOptions.length === 0 || projects.length === 0}
              >
                <UserPlus size={16} />
                {savingAssignment ? 'Guardando...' : 'Asignar usuario'}
              </button>
            </div>
          </article>

          <article className="config-card">
            <div className="section-title compact">
              <div>
                <h2>Asignaciones del usuario</h2>
                <p>Vista resumida de los proyectos y cargos vinculados.</p>
              </div>
            </div>

            <div className="assignment-preview">
              {assignmentPreview.length === 0 ? (
                <p className="empty-state">No hay asignaciones registradas para el usuario seleccionado.</p>
              ) : (
                assignmentPreview.map((item) => (
                  <div key={`${item.proyectoId}-${item.cargo}`} className="assignment-item">
                    <strong>{item.proyectoCodigo || item.proyectoId}</strong>
                    <span>{item.proyectoNombre || item.proyectoId}</span>
                    <small>{item.cargo}</small>
                  </div>
                ))
              )}
            </div>
          </article>
        </section>
      )}
    </div>
  );
};

export default SecurityConfigPage;
