import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '../api/axiosConfig';
import documentService from './documentService';

vi.mock('../api/axiosConfig', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}));

describe('documentService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('normaliza el identificador al cargar documentos', async () => {
    apiClient.post.mockResolvedValue({ data: { success: true } });
    const file = new File(['contenido'], 'cronograma.pdf', { type: 'application/pdf' });

    await documentService.cargarDocumento(' proy-cun-2026-008 ', 'CRONOGRAMA', file);

    expect(apiClient.post).toHaveBeenCalledWith(
      '/proyectos/PROY-CUN-2026-008/documentos/CRONOGRAMA',
      expect.any(FormData),
      expect.any(Object)
    );
  });

  it('normaliza el identificador al listar documentos', async () => {
    apiClient.get.mockResolvedValue({ data: { success: true } });

    await documentService.listarDocumentos('proy-cun-2026-008');

    expect(apiClient.get).toHaveBeenCalledWith('/proyectos/PROY-CUN-2026-008/documentos');
  });
});
