DELETE FROM proyecta_db.rol_permiso rp
USING proyecta_db.roles r, proyecta_db.permisos p
WHERE rp.rol_id = r.id
  AND rp.permiso_id = p.id
  AND p.codigo IN ('DOCUMENTO:HISTORIAL', 'DOCUMENTO:REVERTIR')
  AND LOWER(r.codigo) NOT IN ('admin', 'gestor_proyectos');
