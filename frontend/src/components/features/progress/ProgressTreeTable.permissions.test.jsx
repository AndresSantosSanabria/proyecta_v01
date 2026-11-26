import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import ProgressTreeTable from './ProgressTreeTable';

const auth = vi.hoisted(() => ({
  roles: [],
  isAdminLocal: false,
  transversal: false,
}));

vi.mock('../../../context/AuthContext', () => ({
  useAuthContext: () => ({
    hasRole: (role) => auth.roles.includes(role),
    isAdminLocal: auth.isAdminLocal,
    transversal: auth.transversal,
  }),
}));

vi.mock('../../../hooks/usePermission', () => ({ usePermission: () => true }));
vi.mock('../../../api/axiosConfig', () => ({ default: { get: vi.fn() } }));
vi.mock('../../../services/projectService', () => ({ default: {} }));
vi.mock('../../common/EvidenceUpload', () => ({ default: () => null }));
vi.mock('../wizard/steps/Paso3FasesHitosEntregables', () => ({ default: () => null }));
vi.mock('../../common/SpellCheckerTextarea', () => ({ default: () => null }));
vi.mock('./ModificarFechaModal', () => ({ default: () => null }));
vi.mock('./HistorialCambiosFecha', () => ({ default: () => null }));
vi.mock('./ModificarDescripcionModal', () => ({ default: () => null }));

const progressData = {
  fases: [{
    id: 1,
    nombre: 'Fase 1',
    hitos: [{
      id: 2,
      nombre: 'Hito 1',
      entregables: [{ id: 3, nombre: 'Entregable 1', descripcion: 'Descripcion', fechaLimite: '2026-12-01' }],
    }],
  }],
};

const renderTable = () => render(
  <ProgressTreeTable
    progressData={progressData}
    projectInfo={{}}
    isExpanded={() => true}
    toggleNode={vi.fn()}
  />,
);

beforeEach(() => {
  auth.roles = [];
  auth.isAdminLocal = false;
  auth.transversal = false;
});

describe('permisos para modificar entregables', () => {
  it('muestra fecha y descripcion al gestor con permisos', () => {
    auth.roles = ['GESTOR_PROYECTOS'];
    renderTable();

    expect(screen.getByRole('columnheader', { name: 'Proyecto' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Modificar fecha limite' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Modificar descripción' })).toBeInTheDocument();
  });

  it('no muestra fecha ni descripcion a otro rol aunque tenga el permiso funcional', () => {
    auth.roles = ['DIRECTOR_PROYECTO'];
    renderTable();

    expect(screen.queryByRole('button', { name: 'Modificar fecha limite' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Modificar descripción' })).not.toBeInTheDocument();
  });
});
