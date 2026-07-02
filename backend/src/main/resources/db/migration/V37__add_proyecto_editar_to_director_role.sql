-- Add PROYECTO:EDITAR permission to director_proyecto role
-- so directors can manage risks on their assigned projects

INSERT INTO proyecta_db.rol_permiso (rol_id, permiso_id, activo)
SELECT r.id, p.id, true
FROM proyecta_db.roles r, proyecta_db.permisos p
WHERE r.codigo = 'director_proyecto'
  AND p.codigo = 'PROYECTO:EDITAR'
  AND NOT EXISTS (
      SELECT 1 FROM proyecta_db.rol_permiso rp
      WHERE rp.rol_id = r.id AND rp.permiso_id = p.id
  );
