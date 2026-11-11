import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ProjectOnboardingWizard from './ProjectOnboardingWizard';
import projectService from '../../services/projectService';
import { emitToast } from '../../utils/feedback';

vi.mock('../../services/configCatalogService', () => ({
  default: {
    listarValoresParametrica: vi.fn(() => Promise.resolve([])),
    getPetiCatalog: vi.fn(() => Promise.resolve({ furagPreguntas: [] })),
  },
}));

vi.mock('../../services/projectService', () => ({
  default: {
    getCompletionDraft: vi.fn(() => Promise.resolve(null)),
    saveCompletionDraft: vi.fn(() => Promise.resolve({})),
    completePhase: vi.fn(() => Promise.resolve({})),
    update: vi.fn(() => Promise.resolve({})),
    getPatrocinadores: vi.fn(() => Promise.resolve([])),
  },
}));

vi.mock('../../utils/feedback', () => ({ emitToast: vi.fn() }));

vi.mock('../../hooks/usePermission', () => ({
  usePermission: () => true,
  useProjectAccess: () => true,
}));

vi.mock('../features/wizard/steps/Paso3FasesHitosEntregables', () => ({ default: () => null }));
vi.mock('../features/wizard/steps/Paso4PetiComunicaciones', () => ({ default: () => null }));
vi.mock('../features/wizard/steps/Paso5Furag', () => ({ default: () => null }));
vi.mock('../features/wizard/steps/Paso6MatrizRiesgos', () => ({ default: () => null }));
vi.mock('../features/wizard/steps/Paso6GestionDocumental', () => ({ default: () => null }));

vi.mock('../common/SpellCheckerInput', () => ({
  default: ({ value, onChange, className, placeholder }) => (
    <input className={className} placeholder={placeholder} value={value} onChange={onChange} />
  ),
}));

vi.mock('../common/SpellCheckerTextarea', () => ({
  default: ({ value, onChange, className, placeholder }) => (
    <textarea className={className} placeholder={placeholder} value={value} onChange={onChange} />
  ),
}));

vi.mock('../common/AutocompleteSelect', () => ({
  AutocompleteSelect: ({ value, onChange, className, placeholder }) => (
    <input className={className} placeholder={placeholder} value={value} onChange={(e) => onChange(e.target.value)} />
  ),
}));

const PROJECT = { id: 'PROY-TEST-1', codigo: 'PROY-TEST-1', nombre: 'Proyecto de prueba' };

const baseForm = () => ({
  patrocinador: { nombre: 'Secretaria de TIC', cargo: 'Secretario', procesoSigc: '', procedimientoSigc: '' },
  equipoTrabajo: [{ nombre: 'qqq', cargo: 'qqqqqqq', rol: 'Lider', dependencia: 'nose', telefono: '42342349384', correo: 'integrante@correo.com' }],
  stakeholders: [{ rol: '', descripcion: 'dwedew', interes: 'fefebwfejejw', impacto: 'aqwsedrftgyhuji' }],
});

const seedStep3 = (form) => {
  localStorage.setItem(`wizard_onboarding_${PROJECT.id}`, JSON.stringify({ step: 3, form }));
};

const renderWizard = () => render(<ProjectOnboardingWizard project={PROJECT} onComplete={vi.fn()} />);

const clickNext = () => fireEvent.click(screen.getByRole('button', { name: /Siguiente/ }));

describe('ProjectOnboardingWizard - validacion por campo del paso 3', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('anuncia el campo faltante exacto (no el agregado enganoso) y marca el input', async () => {
    seedStep3(baseForm());
    renderWizard();
    await screen.findByText('Patrocinador del Proyecto');

    clickNext();

    const banner = await screen.findByRole('alert');
    expect(banner.textContent).toContain('Faltan 1 campo(s)');
    expect(banner.textContent).toContain('Stakeholder #1: el rol es obligatorio.');
    expect(banner.textContent).not.toContain('Debe agregar al menos un stakeholder');

    const rolInput = screen.getByPlaceholderText('Ej: Gestores de cuentas de cobro');
    expect(rolInput.className).toContain('input-error');

    expect(emitToast).toHaveBeenCalledWith(expect.objectContaining({
      tone: 'warning',
      title: expect.stringContaining('Faltan 1'),
    }));
    expect(projectService.saveCompletionDraft).not.toHaveBeenCalled();
  });

  it('cuenta multiples campos faltantes entre integrantes y stakeholders', async () => {
    const form = baseForm();
    form.equipoTrabajo[0].rol = '';
    seedStep3(form);
    renderWizard();
    await screen.findByText('Patrocinador del Proyecto');

    clickNext();

    const banner = await screen.findByRole('alert');
    expect(banner.textContent).toContain('Faltan 2 campo(s)');
    expect(banner.textContent).toContain('Integrante #1: seleccione el rol en el proyecto.');
    expect(banner.textContent).toContain('Stakeholder #1: el rol es obligatorio.');
    expect(projectService.saveCompletionDraft).not.toHaveBeenCalled();
  });

  it('limpia la marca en vivo al completar el campo y permite avanzar', async () => {
    seedStep3(baseForm());
    renderWizard();
    await screen.findByText('Patrocinador del Proyecto');

    clickNext();
    await screen.findByRole('alert');

    fireEvent.change(screen.getByPlaceholderText('Ej: Gestores de cuentas de cobro'), {
      target: { value: 'Gestores de cuentas de cobro' },
    });

    await waitFor(() => expect(screen.queryByRole('alert')).toBeNull());
    expect(screen.getByPlaceholderText('Ej: Gestores de cuentas de cobro').className).not.toContain('input-error');

    clickNext();
    await waitFor(() => expect(projectService.saveCompletionDraft).toHaveBeenCalled());
    expect(projectService.completePhase).toHaveBeenCalledWith(PROJECT.id, 3);
    await waitFor(() => expect(screen.queryByText('Patrocinador del Proyecto')).toBeNull());
  });

  it('solo pide agregar stakeholder cuando la lista esta realmente vacia', async () => {
    const form = baseForm();
    form.stakeholders = [];
    seedStep3(form);
    renderWizard();
    await screen.findByText('Patrocinador del Proyecto');

    clickNext();

    const banner = await screen.findByRole('alert');
    expect(banner.textContent).toContain('Debe agregar al menos un stakeholder.');
    expect(projectService.saveCompletionDraft).not.toHaveBeenCalled();
  });

  it('valida el formato del correo del integrante cuando se diligencia', async () => {
    const form = baseForm();
    form.stakeholders[0].rol = 'Gestores';
    form.equipoTrabajo[0].correo = 'correo-invalido';
    seedStep3(form);
    renderWizard();
    await screen.findByText('Patrocinador del Proyecto');

    clickNext();

    const banner = await screen.findByRole('alert');
    expect(banner.textContent).toContain('Integrante #1: el correo electrónico no es válido.');
    expect(screen.getByPlaceholderText('correo@ejemplo.com').className).toContain('input-error');
    expect(projectService.saveCompletionDraft).not.toHaveBeenCalled();
  });

  it('exige el telefono del integrante', async () => {
    const form = baseForm();
    form.stakeholders[0].rol = 'Gestores';
    form.equipoTrabajo[0].telefono = '';
    seedStep3(form);
    renderWizard();
    await screen.findByText('Patrocinador del Proyecto');

    clickNext();

    const banner = await screen.findByRole('alert');
    expect(banner.textContent).toContain('Integrante #1: el teléfono es obligatorio.');
    expect(screen.getByPlaceholderText('Solo números').className).toContain('input-error');
    expect(projectService.saveCompletionDraft).not.toHaveBeenCalled();
  });

  it('exige el correo del integrante cuando esta vacio', async () => {
    const form = baseForm();
    form.stakeholders[0].rol = 'Gestores';
    form.equipoTrabajo[0].correo = '';
    seedStep3(form);
    renderWizard();
    await screen.findByText('Patrocinador del Proyecto');

    clickNext();

    const banner = await screen.findByRole('alert');
    expect(banner.textContent).toContain('Integrante #1: el correo electrónico es obligatorio.');
    expect(screen.getByPlaceholderText('correo@ejemplo.com').className).toContain('input-error');
    expect(projectService.saveCompletionDraft).not.toHaveBeenCalled();
  });
});
