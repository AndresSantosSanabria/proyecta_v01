import { render, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import ProjectListTable from './ProjectListTable';

vi.hoisted(() => {
  window.__APP_CONFIG__ = {
    KEYCLOAK_BASE_URL: 'http://localhost:8080',
    KEYCLOAK_REALM: 'test',
    KEYCLOAK_CLIENT_ID: 'proyecta-test',
  };
});

const baseProject = {
  id: 'PROY-CUN-2026-001',
  codigo: 'PROY-CUN-2026-001',
  nombre: 'Proyecto de prueba',
  dependencia: 'Secretaría Jurídica',
  director: 'Director Prueba',
  estado: 'ACTIVO',
  avanceTotal: 10,
};

const renderTable = (projects) =>
  render(
    <MemoryRouter>
      <ProjectListTable projects={projects} />
    </MemoryRouter>
  );

const desktopTable = (container) => within(container.querySelector('.project-list-table'));

describe('ProjectListTable PETI badge', () => {
  it('muestra PETI cuando el proyecto está definido como PETI', () => {
    const { container } = renderTable([{ ...baseProject, peti: true, petiDefinido: true }]);
    const badge = desktopTable(container).getByText('PETI', { selector: '.peti-badge' });
    expect(badge).toHaveClass('peti-badge');
    expect(badge).not.toHaveClass('no-peti');
    expect(badge).not.toHaveClass('peti-undefined');
  });

  it('muestra No PETI cuando el proyecto completó su información y no es PETI', () => {
    const { container } = renderTable([{ ...baseProject, peti: false, petiDefinido: true }]);
    const badge = desktopTable(container).getByText('No PETI');
    expect(badge).toHaveClass('no-peti');
  });

  it('muestra Sin definir cuando el proyecto no ha completado su información', () => {
    const { container } = renderTable([
      { ...baseProject, estado: 'PENDIENTE_COMPLETAR', peti: false, petiDefinido: false },
    ]);
    const badge = desktopTable(container).getByText('Sin definir');
    expect(badge).toHaveClass('peti-undefined');
    expect(desktopTable(container).queryByText('No PETI')).not.toBeInTheDocument();
  });

  it('muestra Sin definir para cierre forzoso sin información completada', () => {
    const { container } = renderTable([
      { ...baseProject, estado: 'CERRADO_FORZOSO', peti: false, petiDefinido: false },
    ]);
    expect(desktopTable(container).getByText('Sin definir')).toHaveClass('peti-undefined');
  });

  it('mantiene el comportamiento legado si petiDefinido no viene en la respuesta', () => {
    const { container } = renderTable([{ ...baseProject, peti: false }]);
    expect(desktopTable(container).getByText('No PETI')).toHaveClass('no-peti');
  });

  it('muestra Sin definir en la tarjeta móvil', () => {
    const { container } = renderTable([
      { ...baseProject, estado: 'PENDIENTE_COMPLETAR', peti: false, petiDefinido: false },
    ]);
    const cards = within(container.querySelector('.project-list-cards'));
    expect(cards.getByText('Sin definir')).toBeInTheDocument();
  });
});