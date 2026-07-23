UPDATE proyecta_db.lista_parametrica_config
SET lista_nombre_campo = 'Dependencia y/o Secretaria Responsable',
    lista_descripcion = 'Dependencias o secretarias que pueden ser asignadas como responsables del proyecto',
    lista_tipo = 'Lista'
WHERE lista_clave = 'DEPENDENCIA';

UPDATE proyecta_db.lista_parametrica_config
SET lista_nombre_campo = 'Rol en el Proyecto',
    lista_descripcion = 'Roles que pueden desempenar los miembros del equipo de trabajo',
    lista_tipo = 'Lista'
WHERE lista_clave = 'ROL_EQUIPO';

UPDATE proyecta_db.lista_parametrica_config
SET lista_nombre_campo = 'Vigencia PETI',
    lista_descripcion = 'Vigencias del Plan Estrategico de Tecnologias de la Informacion',
    lista_tipo = 'Lista'
WHERE lista_clave = 'VIGENCIA_PETI';

UPDATE proyecta_db.lista_parametrica_config
SET lista_nombre_campo = 'Estrategia PETI',
    lista_descripcion = 'Estrategias del Plan Estrategico de Tecnologias de la Informacion',
    lista_tipo = 'Lista'
WHERE lista_clave = 'ESTRATEGIA_PETI';

UPDATE proyecta_db.lista_parametrica_config
SET lista_nombre_campo = 'Preguntas FURAG',
    lista_descripcion = 'Preguntas del Formulario Unico de Reporte de Avance de la Gestion',
    lista_tipo = 'Lista'
WHERE lista_clave = 'FURAG_PREGUNTAS';
