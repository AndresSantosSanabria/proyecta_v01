import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, fireEvent, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { DocumentosPreWizardView } from './ProjectLifecycleGuard';
import documentService from '../../services/documentService';
import { emitToast } from '../../utils/feedback';

vi.hoisted(() => {
  window.__APP_CONFIG__ = {
    KEYCLOAK_BASE_URL: 'http://localhost:8080',
    KEYCLOAK_REALM: 'test',
    KEYCLOAK_CLIENT_ID: 'proyecta-test',
  };
});

vi.mock('../../services/documentService', () => ({
  default: { cargarDocumento: vi.fn(() => Promise.resolve({})) },
}));

vi.mock('../../utils/feedback', async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, emitToast: vi.fn() };
});

const pdf = (name) => new File(['contenido'], name, { type: 'application/pdf' });

const project = { id: 'PROY-TEST-001', nombre: 'Proyecto de prueba', director: 'Director' };

const renderView = (props = {}) =>
  render(
    <MemoryRouter>
      <DocumentosPreWizardView project={project} completionStatus={{}} onComplete={vi.fn()} {...props} />
    </MemoryRouter>
  );

const submitButton = (container) =>
  within(container).getByRole('button', { name: /Cargar Documentos/i });

const placeholders = (container) => container.querySelectorAll('.plg-doc-card__placeholder');

const dropPdf = (el, name) => fireEvent.drop(el, { dataTransfer: { files: [pdf(name)] } });

describe('DocumentosPreWizardView - validacion de documentos requeridos', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('mantiene el boton Cargar Documentos activo aunque falten archivos', () => {
    const { container } = renderView();
    expect(submitButton(container).disabled).toBe(false);
  });

  it('al enviar sin documentos informa los faltantes, los marca y no avanza', () => {
    const onComplete = vi.fn();
    const { container } = renderView({ onComplete });

    fireEvent.click(submitButton(container));

    expect(container.querySelectorAll('.plg-doc-card--missing').length).toBe(3);
    expect(container.querySelectorAll('.plg-doc-card__missing-msg').length).toBe(3);
    expect(container.querySelector('.plg-missing-banner')).not.toBeNull();
    expect(emitToast).toHaveBeenCalledWith(
      expect.objectContaining({ tone: 'warning', title: 'Documentos requeridos faltantes' })
    );
    expect(documentService.cargarDocumento).not.toHaveBeenCalled();
    expect(onComplete).not.toHaveBeenCalled();
  });

  it('con carga parcial sigue bloqueado y marca solo los pendientes', () => {
    const onComplete = vi.fn();
    const { container } = renderView({ onComplete });

    dropPdf(placeholders(container)[0], 'viabilidad.pdf');
    fireEvent.click(submitButton(container));

    expect(container.querySelectorAll('.plg-doc-card--missing').length).toBe(2);
    expect(onComplete).not.toHaveBeenCalled();
    expect(documentService.cargarDocumento).not.toHaveBeenCalled();
  });

  it('al seleccionar un archivo marcado limpia su marca en vivo', () => {
    const { container } = renderView();
    fireEvent.click(submitButton(container));
    expect(container.querySelectorAll('.plg-doc-card--missing').length).toBe(3);

    dropPdf(placeholders(container)[0], 'viabilidad.pdf');
    expect(container.querySelectorAll('.plg-doc-card--missing').length).toBe(2);
  });

  it('con los 3 documentos cargados envia y continua el flujo', async () => {
    const onComplete = vi.fn();
    const { container } = renderView({ onComplete });

    const slots = placeholders(container);
    dropPdf(slots[0], 'viabilidad.pdf');
    dropPdf(slots[1], 'plan.pdf');
    dropPdf(slots[2], 'matriz.pdf');

    expect(container.querySelector('.plg-missing-banner')).toBeNull();
    fireEvent.click(submitButton(container));

    await waitFor(() => expect(onComplete).toHaveBeenCalledTimes(1));
    expect(documentService.cargarDocumento).toHaveBeenCalledTimes(3);
    expect(container.querySelectorAll('.plg-doc-card--missing').length).toBe(0);
  });
});
