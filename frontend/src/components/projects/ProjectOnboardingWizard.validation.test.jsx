import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ProjectOnboardingWizard from './ProjectOnboardingWizard';
import projectService from '../../services/projectService';
import { emitToast } from '../../utils/feedback';

vi.mock('../../services/configCatalogService', () => ({
  default: {
    listarValoresParametrica: vi.fn(() => Promise.resolve([])),
    getPetiCatalog: vi.fn(() => Promise.resolve({ furagPreguntas: [{ key: 'P1', label: 'Pregunta FURAG 1' }] })),
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

vi.mock('../../context/AuthContext', () => ({
  useAuthContext: () => ({
    hasRole: () => false,
    isAdminLocal: false,
    transversal: true,
  }),
}));

vi.mock('../features/wizard/steps/Paso3FasesHitosEntregables', () => ({ default: () => null }));
vi.mock('../features/wizard/steps/Paso4PetiComunicaciones', () => ({ default: () => null }));
vi.mock('../features/wizard/steps/Paso5Furag', () => ({
  default: ({ data, onChange, preguntas }) => (
    <div>
      <h3>Cuestionario FURAG</h3>
      {preguntas?.map((pregunta) => (
        <label key={pregunta.key}>
          <input
            type="radio"
            name={`furag_${pregunta.key}`}
            checked={data.furag?.[pregunta.key] === 'SI'}
            onChange={() => onChange({ furag: { ...data.furag, [pregunta.key]: 'SI' } })}
          />
          Sí
        </label>
      ))}
      {preguntas?.map((pregunta) => (
        <label key={`${pregunta.key}-no`}>
          <input
            type="radio"
            name={`furag_${pregunta.key}`}
            checked={data.furag?.[pregunta.key] === 'NO'}
            onChange={() => onChange({ furag: { ...data.furag, [pregunta.key]: 'NO' } })}
          />
          No
        </label>
      ))}
    </div>
  ),
}));
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
  actaConstitucionPdf: { name: 'acta.pdf' },
  cronogramaPdf: { name: 'cronograma.pdf' },
  dependencia: 'Secretaria de TIC',
  alcanceDetallado: 'Alcance del proyecto de prueba',
  presupuestoEstimado: 1000,
  objetivosEspecificos: ['Objetivo del proyecto de prueba'],
  patrocinador: { nombre: 'Secretaria de TIC', cargo: 'Secretario', procesoSigc: '', procedimientoSigc: '' },
  equipoTrabajo: [{ nombre: 'qqq', cargo: 'qqqqqqq', rol: 'Lider', dependencia: 'nose', telefono: '42342349384', correo: 'integrante@correo.com' }],
  stakeholders: [{ rol: '', descripcion: 'dwedew', interes: 'fefebwfejejw', impacto: 'aqwsedrftgyhuji' }],
});

const seedStep3 = (form) => {
  localStorage.setItem(`wizard_onboarding_${PROJECT.id}`, JSON.stringify({ step: 3, form }));
};

const renderWizard = () => render(<ProjectOnboardingWizard project={PROJECT} onComplete={vi.fn()} />);

const clickComplete = () => fireEvent.click(screen.getByRole('button', { name: 'Completar proyecto' }));

describe('ProjectOnboardingWizard - validacion por campo del paso 3', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('anuncia el campo faltante exacto (no el agregado enganoso) y marca el input', async () => {
    seedStep3(baseForm());
    renderWizard();
    await screen.findByText('Patrocinador del Proyecto');

    clickComplete();

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

    clickComplete();

    const banner = await screen.findByRole('alert');
    expect(banner.textContent).toContain('Faltan 2 campo(s)');
    expect(banner.textContent).toContain('Integrante #1: seleccione el rol en el proyecto.');
    expect(banner.textContent).toContain('Stakeholder #1: el rol es obligatorio.');
    expect(projectService.saveCompletionDraft).not.toHaveBeenCalled();
  });

  it('limpia la marca en vivo y continua con la validacion del siguiente momento al completar', async () => {
    seedStep3(baseForm());
    renderWizard();
    await screen.findByText('Patrocinador del Proyecto');

    clickComplete();
    await screen.findByRole('alert');

    fireEvent.change(screen.getByPlaceholderText('Ej: Gestores de cuentas de cobro'), {
      target: { value: 'Gestores de cuentas de cobro' },
    });

    await waitFor(() => expect(screen.queryByRole('alert')).toBeNull());
    expect(screen.getByPlaceholderText('Ej: Gestores de cuentas de cobro').className).not.toContain('input-error');

    clickComplete();
    await screen.findByText('Paso 4 de 7');
    expect(screen.getByRole('alert').textContent).toContain('Debe configurar al menos una fase con hito y entregable.');
    expect(projectService.saveCompletionDraft).not.toHaveBeenCalled();
    expect(projectService.completePhase).not.toHaveBeenCalled();
  });

  it('exige la descripcion de cada entregable al completar', async () => {
    const form = baseForm();
    form.stakeholders[0].rol = 'Gestores de cuentas de cobro';
    form.fases = [{
      nombre: 'F01', descripcion: '', ponderacion: 100,
      hitos: [{
        nombre: 'H01', descripcion: '', ponderacion: 100,
        entregables: [{
          nombre: 'E01', descripcion: '   ', ponderacion: 100,
          fechaInicio: '2026-01-01', fechaLimite: '2026-12-31',
        }],
      }],
    }];
    seedStep3(form);
    renderWizard();
    await screen.findByText('Patrocinador del Proyecto');

    clickComplete();
    await screen.findByText('Paso 4 de 7');
    clickComplete();

    const banner = await screen.findByRole('alert');
    expect(banner.textContent).toContain('La descripción del entregable es obligatoria.');
    expect(projectService.saveCompletionDraft).not.toHaveBeenCalled();
  });

  it('solo pide agregar stakeholder cuando la lista esta realmente vacia', async () => {
    const form = baseForm();
    form.stakeholders = [];
    seedStep3(form);
    renderWizard();
    await screen.findByText('Patrocinador del Proyecto');

    clickComplete();

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

    clickComplete();

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

    clickComplete();

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

    clickComplete();

    const banner = await screen.findByRole('alert');
    expect(banner.textContent).toContain('Integrante #1: el correo electrónico es obligatorio.');
    expect(screen.getByPlaceholderText('correo@ejemplo.com').className).toContain('input-error');
    expect(projectService.saveCompletionDraft).not.toHaveBeenCalled();
  });
});

describe('ProjectOnboardingWizard - navegacion libre', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('permite saltar entre cualquier par de momentos sin Siguiente ni Volver', async () => {
    localStorage.setItem(`wizard_onboarding_${PROJECT.id}`, JSON.stringify({ step: 1, form: baseForm() }));
    renderWizard();
    await screen.findByText('Paso 1 de 7');

    expect(screen.queryByRole('button', { name: /Siguiente/ })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Volver' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Completar proyecto' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '07 Matriz de riesgos' }));
    await screen.findByText('Paso 7 de 7');

    expect(screen.getByRole('button', { name: '07 Matriz de riesgos' })).toHaveAttribute('aria-current', 'step');
    expect(screen.getByRole('button', { name: 'Completar proyecto' })).toBeInTheDocument();
    expect(JSON.parse(localStorage.getItem(`wizard_onboarding_${PROJECT.id}`)).step).toBe(7);
    expect(projectService.saveCompletionDraft).not.toHaveBeenCalled();
    expect(projectService.completePhase).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: '03 Patrocinador y equipo' }));
    await screen.findByText('Patrocinador del Proyecto');

    expect(screen.getByText('Paso 3 de 7')).toBeInTheDocument();
    expect(JSON.parse(localStorage.getItem(`wizard_onboarding_${PROJECT.id}`)).step).toBe(3);
  });

  it('valida todos los momentos al completar y lleva al primer paso pendiente', async () => {
    const form = baseForm();
    form.actaConstitucionPdf = null;
    form.cronogramaPdf = null;
    localStorage.setItem(`wizard_onboarding_${PROJECT.id}`, JSON.stringify({ step: 7, form }));
    const onComplete = vi.fn();

    render(<ProjectOnboardingWizard project={PROJECT} onComplete={onComplete} />);
    await screen.findByText('Paso 7 de 7');
    clickComplete();

    await screen.findByText('Paso 1 de 7');
    const banner = await screen.findByRole('alert');
    expect(banner.textContent).toContain('El Acta de Constitución es obligatoria.');
    expect(banner.textContent).toContain('El Cronograma del Proyecto es obligatorio.');
    expect(onComplete).not.toHaveBeenCalled();
  });
});

describe('ProjectOnboardingWizard - persistencia FURAG', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
    projectService.getCompletionDraft.mockResolvedValue(null);
    projectService.saveCompletionDraft.mockResolvedValue({});
  });

  it('restaura las respuestas anidadas del borrador del backend', async () => {
    projectService.getCompletionDraft.mockResolvedValue({
      data: { data: { faseActual: 6, datosFase5: { furag: { respuestas: { P1: 'NO' } } } } },
    });

    renderWizard();
    await screen.findByText('Cuestionario FURAG');

    expect(screen.getByRole('radio', { name: 'No' })).toBeChecked();
    expect(screen.getByText('Paso 6 de 7')).toBeInTheDocument();
  });

  it('conserva las respuestas locales aunque el backend devuelva un borrador vacio', async () => {
    const form = { ...baseForm(), furag: { P1: 'SI' } };
    localStorage.setItem(`wizard_onboarding_${PROJECT.id}`, JSON.stringify({ step: 6, form }));
    projectService.getCompletionDraft.mockResolvedValue({
      data: { data: { faseActual: 1, datosFase5: { furag: { respuestas: {} } } } },
    });

    renderWizard();
    await screen.findByText('Cuestionario FURAG');

    expect(screen.getByRole('radio', { name: 'Sí' })).toBeChecked();
    expect(screen.getByText('Paso 6 de 7')).toBeInTheDocument();
  });

  it('guarda las respuestas inmediatamente y envia la estructura esperada al cerrar', async () => {
    const form = { ...baseForm(), furag: {} };
    localStorage.setItem(`wizard_onboarding_${PROJECT.id}`, JSON.stringify({ step: 6, form }));
    renderWizard();
    await screen.findByText('Cuestionario FURAG');

    fireEvent.click(screen.getByRole('radio', { name: 'No' }));
    expect(JSON.parse(localStorage.getItem(`wizard_onboarding_${PROJECT.id}`)).form.furag).toEqual({ P1: 'NO' });

    fireEvent.click(screen.getByTitle('Cerrar y guardar progreso'));
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar y guardar' }));

    await waitFor(() => expect(projectService.saveCompletionDraft).toHaveBeenCalled());
    const payload = projectService.saveCompletionDraft.mock.calls[0][1];
    expect(payload.datosFase5.furag).toEqual({ respuestas: { P1: 'NO' } });
    await waitFor(() => expect(localStorage.getItem(`wizard_onboarding_${PROJECT.id}`)).toBeNull());
  });

  it('conserva el respaldo local si falla el guardado del borrador al cerrar', async () => {
    const form = { ...baseForm(), furag: { P1: 'SI' } };
    localStorage.setItem(`wizard_onboarding_${PROJECT.id}`, JSON.stringify({ step: 6, form }));
    projectService.saveCompletionDraft.mockRejectedValue(new Error('Sin conexion'));
    vi.spyOn(console, 'error').mockImplementation(() => {});

    renderWizard();
    await screen.findByText('Cuestionario FURAG');
    fireEvent.click(screen.getByTitle('Cerrar y guardar progreso'));
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar y guardar' }));

    await waitFor(() => expect(projectService.saveCompletionDraft).toHaveBeenCalled());
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Cerrar y guardar' })).toBeNull());
    expect(JSON.parse(localStorage.getItem(`wizard_onboarding_${PROJECT.id}`)).form.furag).toEqual({ P1: 'SI' });
  });
});
