import { describe, expect, it } from 'vitest';
import { formatDate, formatDateTime } from './locale';

describe('formatDate', () => {
  it('muestra la fecha de calendario tal cual llega de la API (sin zona horaria)', () => {
    expect(formatDate('2026-11-18')).toBe('18 de noviembre de 2026');
    expect(formatDate('2026-11-05')).toBe('5 de noviembre de 2026');
  });

  it('no resta un dia al convertir a America/Bogota', () => {
    expect(formatDate('2026-01-01')).toBe('1 de enero de 2026');
  });

  it('devuelve vacio para valores ausentes o invalidos', () => {
    expect(formatDate(null)).toBe('');
    expect(formatDate('')).toBe('');
    expect(formatDate('no-es-fecha')).toBe('');
  });
});

describe('formatDateTime', () => {
  it('convierte instantes con zona horaria a America/Bogota', () => {
    const formateada = formatDateTime('2026-11-10T19:04:07Z');
    expect(formateada).toContain('10 de noviembre de 2026');
    expect(formateada).toContain('14:04:07');
  });
});