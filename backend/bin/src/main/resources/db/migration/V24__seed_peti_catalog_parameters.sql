ALTER TABLE system_parameters
    ALTER COLUMN param_value TYPE VARCHAR(1000);

INSERT INTO system_parameters (param_key, param_value, descripcion)
VALUES (
    'peti_vigencias',
    '2020-2024,2024-2027,2027-2030',
    'Vigencias disponibles para el catalogo PETI'
)
ON CONFLICT (param_key) DO NOTHING;

INSERT INTO system_parameters (param_key, param_value, descripcion)
VALUES (
    'peti_estrategias',
    'TECNOLOGIAS_INFORMACION:Tecnologias de la Informacion|TRANSFORMACION_DIGITAL:Transformacion Digital|CIUDADES_TERRITORIOS_INTELIGENTES:Ciudades y Territorios Inteligentes|GOBIERNO_DIGITAL:Gobierno Digital',
    'Estrategias PETI disponibles. Formato: CODIGO:Nombre separadas por |'
)
ON CONFLICT (param_key) DO NOTHING;
