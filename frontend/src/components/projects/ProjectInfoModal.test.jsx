import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import ProjectInfoModal from './ProjectInfoModal';
import documentService from '../../services/documentService';
import projectService from '../../services/projectService';
import configCatalogService from '../../services/configCatalogService';

vi.mock('../../services/documentService', () => ({
  default: {
    listarDocumentos: vi.fn(),
    listarVersiones: vi.fn(),
    descargarDocumento: vi.fn(),
    descargarVersion: vi.fn(),
  },
}));

vi.mock('../../services/projectService', () => ({
  default: {
    getFurag: vi.fn(),
    update: vi.fn(),
    updateFurag: vi.fn(),
  },
}));

vi.mock('../../services/configCatalogService', () => ({
  default: {
    getPetiCatalog: vi.fn(),
  },
}));

const project = {
  id: 'PROY-TEST-1',
  codigo: 'PROY-TEST-1',
  nombre: 'Proyecto de prueba',
  dependencia: 'Secretaria de TIC',
  fases: [],
};

const renderModal = () => render(
  <ProjectInfoModal project={project} open onClose={vi.fn()} />,
);

beforeEach(() => {
  vi.clearAllMocks();
  documentService.listarDocumentos.mockResolvedValue({
    data: { documentos: [{ tipoDocumentoCodigo: 'VIABILIZACION' }] },
  });
  documentService.listarVersiones.mockResolvedValue({ data: { versiones: [{ id: 1, numeroVersion: 1 }] } });
  projectService.getFurag.mockResolvedValue({ data: { respuestas: {} } });
  configCatalogService.getPetiCatalog.mockResolvedValue({ furagPreguntas: [] });
});

describe('ProjectInfoModal solo lectura', () => {
  it('no ofrece controles para editar la informacion ni reemplazar documentos', async () => {
    const { container } = renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'FURAG' }));
    fireEvent.click(screen.getByRole('button', { name: 'Documentos' }));
    await waitFor(() => expect(screen.getByRole('button', { name: /documento de viabilidad/i })).toBeInTheDocument());

    expect(screen.queryByRole('button', { name: /modificar|editar|guardar|reemplazar|cargar documento/i })).not.toBeInTheDocument();
    expect(container.querySelector('input, textarea, select')).not.toBeInTheDocument();
    expect(projectService.update).not.toHaveBeenCalled();
    expect(projectService.updateFurag).not.toHaveBeenCalled();
  });

  it('conserva consulta, descarga e historial del documento existente', async () => {
    renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Documentos' }));
    const documentButton = await screen.findByRole('button', { name: /documento de viabilidad/i });
    expect(screen.getByTitle('Descargar')).toBeInTheDocument();
    expect(screen.getByTitle('Historial de versiones')).toBeInTheDocument();

    fireEvent.click(documentButton);
    await waitFor(() => expect(documentService.descargarDocumento).toHaveBeenCalledWith('PROY-TEST-1', 'VIABILIZACION'));

    fireEvent.click(screen.getByTitle('Historial de versiones'));
    await waitFor(() => expect(documentService.listarVersiones).toHaveBeenCalledWith('PROY-TEST-1', 'VIABILIZACION'));
  });

  it('muestra los documentos ausentes como no disponibles', async () => {
    documentService.listarDocumentos.mockResolvedValue({ data: { documentos: [] } });
    renderModal();

    fireEvent.click(screen.getByRole('button', { name: 'Documentos' }));

    await waitFor(() => expect(screen.getAllByText('No disponible')).toHaveLength(4));
    expect(screen.queryByTitle('Historial de versiones')).not.toBeInTheDocument();
  });
});
