INSERT INTO system_parameters (param_key, param_value, descripcion)
VALUES (
    'seguridad_role_aliases',
    'ADMINISTRADOR:ADMIN,DIRECTOR_PRO:DIRECTOR_PROYECTO,GESTOR_PROYECTOS_TI:GESTOR_TIC,ANALISTA_PROYECTOS:CONSULTA',
    'Alias de roles externos normalizados contra roles funcionales internos'
)
ON CONFLICT (param_key) DO NOTHING;
