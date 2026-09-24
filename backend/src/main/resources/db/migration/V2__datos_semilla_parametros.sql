-- =============================================================================
-- PROYECTA — Datos Semilla (Esquema Normalizado Sin Duplicidades)
-- Fecha: 2026-09-21
-- Base de datos: PostgreSQL 15+
-- =============================================================================

-- =============================================================================
-- 1. ROLES DEL SISTEMA
-- =============================================================================

INSERT INTO roles (codigo, nombre, descripcion, transversal) VALUES
('admin',                'Administrador',        'Acceso total al sistema',                    TRUE),
('gestor_tic',           'Gestor TIC',           'Gestiona proyectos tecnológicos',            FALSE),
('director_proyecto',    'Director de Proyecto',  'Dirige y supervisa un proyecto específico',  FALSE),
('auditor',              'Auditor',              'Solo lectura con acceso a auditoría',        FALSE),
('consulta',             'Consulta',             'Solo lectura del portafolio',                FALSE),
('visualizador',         'Visualizador',          'Acceso limitado de solo lectura',            FALSE);

-- =============================================================================
-- 2. PERMISOS
-- =============================================================================

INSERT INTO permisos (codigo, nombre, descripcion) VALUES
('DASHBOARD:VER',                        'Ver Dashboard',                       'Acceso al panel principal'),
('DASHBOARD:VER_ANALITICA',              'Ver Analítica',                       'Acceso a analítica del portafolio'),
('PROYECTO:VER',                         'Ver Proyectos',                       'Listar y consultar proyectos'),
('PROYECTO:CREAR',                       'Crear Proyecto',                      'Crear nuevos proyectos'),
('PROYECTO:EDITAR',                      'Editar Proyecto',                     'Modificar información del proyecto'),
('PROYECTO:ELIMINAR',                    'Eliminar Proyecto',                   'Eliminar proyectos'),
('PROYECTO:EDITAR_DATOS_GENERALES',      'Editar Datos Generales',             'Modificar datos generales del proyecto'),
('PROYECTO:EDITAR_FURAG',               'Editar FURAG',                        'Modificar respuestas FURAG'),
('PROYECTO:EDITAR_BENEFICIO_IMPACTO',    'Editar Beneficio/Impacto',           'Modificar beneficio e impacto'),
('ENTREGABLE:VER',                       'Ver Entregables',                     'Consultar entregables'),
('ENTREGABLE:EDITAR',                    'Editar Entregables',                  'Modificar entregables'),
('ENTREGABLE:CAMBIAR_FECHA',             'Cambiar Fecha Entregable',            'Cambiar fechas de entregables'),
('ENTREGABLE:CAMBIAR_DESCRIPCION',       'Cambiar Descripción Entregable',      'Cambiar descripción de entregables'),
('AVANCE:VER',                           'Ver Avance',                          'Consultar avance del proyecto'),
('AVANCE:EDITAR',                        'Editar Avance',                       'Modificar avance del proyecto'),
('EVIDENCIA:VER',                        'Ver Evidencias',                      'Consultar evidencias'),
('EVIDENCIA:SUBIR',                      'Subir Evidencias',                    'Subir archivos de evidencia'),
('EVIDENCIA:APROBAR',                    'Aprobar Evidencias',                  'Aprobar evidencias subidas'),
('DOCUMENTO:VER',                        'Ver Documentos',                      'Consultar documentos del proyecto'),
('DOCUMENTO:SUBIR',                      'Subir Documentos',                    'Subir documentos al proyecto'),
('CRONOGRAMA:VER',                       'Ver Cronograma',                      'Consultar cronograma'),
('CRONOGRAMA:EDITAR',                    'Editar Cronograma',                   'Modificar cronograma'),
('CIERRE:VER',                           'Ver Cierre',                          'Consultar cierre de proyecto'),
('CIERRE:EDITAR',                        'Editar Cierre',                       'Modificar cierre de proyecto'),
('CIERRE:SOLICITAR',                     'Solicitar Cierre',                    'Solicitar cierre de proyecto'),
('CIERRE:EXTRAORDINARIO',                'Cierre Extraordinario',               'Realizar cierre extraordinario'),
('REPORTE:VER',                          'Ver Reportes',                        'Acceder a reportes'),
('REPORTE:CONFIGURAR',                   'Configurar Reportes',                 'Configurar parámetros de reportes'),
('REPORTE:DESCARGAR_ACTUAL',             'Descargar Reporte Actual',            'Descargar reporte en estado actual'),
('ANALITICA:VER',                        'Ver Analítica',                       'Acceso a analítica'),
('CONFIGURACION:VER',                    'Ver Configuración',                   'Consultar configuración del sistema'),
('CONFIGURACION:EDITAR',                 'Editar Configuración',                'Modificar configuración del sistema'),
('SISTEMA:VER_AUDITORIA',                'Ver Auditoría',                       'Consultar logs de auditoría'),
('SISTEMA:GESTIONAR_USUARIOS',           'Gestionar Usuarios',                  'Administrar usuarios del sistema'),
('SIDEBAR:VER_ADMINISTRACION',           'Ver Menú Administración',             'Visible en menú de administración'),
('SIDEBAR:VER_CONFIGURACION',            'Ver Menú Configuración',              'Visible en menú de configuración'),
('DOCUMENTO_INTERNO:VER',                'Ver Documentación Interna',           'Consultar documentación interna del sistema'),
('DOCUMENTO_INTERNO:CARGAR',             'Cargar Documentación Interna',        'Subir archivos de documentación interna'),
('SIDEBAR:DOCUMENTACION_INTERNA',        'Mostrar Documentación Interna en menú', 'Controla la visibilidad del módulo de documentación interna en el sidebar');

-- =============================================================================
-- 3. ASIGNACIÓN DE PERMISOS A ROLES
-- =============================================================================

-- Admin: todos los permisos
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p WHERE r.codigo = 'admin';

-- Gestor TIC: acceso amplio
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.codigo = 'gestor_tic' AND p.codigo NOT IN ('SISTEMA:GESTIONAR_USUARIOS');

-- Director de Proyecto
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.codigo = 'director_proyecto' AND p.codigo IN (
    'DASHBOARD:VER', 'PROYECTO:VER', 'PROYECTO:EDITAR', 'PROYECTO:EDITAR_DATOS_GENERALES',
    'PROYECTO:EDITAR_FURAG', 'PROYECTO:EDITAR_BENEFICIO_IMPACTO',
    'ENTREGABLE:VER', 'ENTREGABLE:EDITAR', 'ENTREGABLE:CAMBIAR_FECHA', 'ENTREGABLE:CAMBIAR_DESCRIPCION',
    'AVANCE:VER', 'AVANCE:EDITAR', 'EVIDENCIA:VER', 'EVIDENCIA:SUBIR', 'EVIDENCIA:APROBAR',
    'DOCUMENTO:VER', 'DOCUMENTO:SUBIR', 'CRONOGRAMA:VER', 'CRONOGRAMA:EDITAR',
    'CIERRE:VER', 'CIERRE:EDITAR', 'CIERRE:SOLICITAR',
    'REPORTE:VER', 'REPORTE:DESCARGAR_ACTUAL'
);

-- Auditor: solo lectura + auditoría
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.codigo = 'auditor' AND p.codigo IN (
    'DASHBOARD:VER', 'DASHBOARD:VER_ANALITICA', 'PROYECTO:VER', 'ENTREGABLE:VER',
    'AVANCE:VER', 'EVIDENCIA:VER', 'DOCUMENTO:VER', 'CRONOGRAMA:VER',
    'CIERRE:VER', 'REPORTE:VER', 'ANALITICA:VER', 'SISTEMA:VER_AUDITORIA'
);

-- Consulta: solo lectura básica
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.codigo = 'consulta' AND p.codigo IN (
    'DASHBOARD:VER', 'PROYECTO:VER', 'ENTREGABLE:VER', 'AVANCE:VER',
    'EVIDENCIA:VER', 'DOCUMENTO:VER', 'CRONOGRAMA:VER', 'CIERRE:VER', 'REPORTE:VER'
);

-- Visualizador: solo vista de proyectos, sin acciones
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.codigo = 'visualizador' AND p.codigo IN ('PROYECTO:VER', 'SIDEBAR:PROYECTOS');

-- =============================================================================
-- 4. ESTADOS DE PROYECTO
-- =============================================================================

INSERT INTO estado_proyecto_config (codigo, nombre, descripcion, color_hex, es_terminal, orden) VALUES
('PENDIENTE_COMPLETAR', 'Pendiente de Completar', 'Proyecto registrado, esperando información inicial', '#6C757D', FALSE, 1),
('PLANIFICACION',       'En Planificación',       'Proyecto en fase de planificación',                '#0D6EFD', FALSE, 2),
('ACTIVO',              'Activo',                  'Proyecto en ejecución activa',                     '#198754', FALSE, 3),
('CON_RETRASOS',        'Con Retrasos',            'Proyecto con entregables atrasados',               '#FFC107', FALSE, 4),
('EN_REVISION',         'En Revisión',             'Proyecto en proceso de revisión',                  '#0DCAF0', FALSE, 5),
('FINALIZADO',          'Finalizado',              'Proyecto completado y aprobado',                   '#28A745', TRUE,  6),
('CERRADO',             'Cerrado',                 'Proyecto cerrado formalmente',                     '#6C757D', TRUE,  7),
('CERRADO_FORZOSO',     'Cerrado Forzosamente',    'Proyecto cerrado de forma obligatoria',            '#DC3545', TRUE,  8);

-- =============================================================================
-- 5. ESTADOS DE ENTREGABLE
-- =============================================================================

INSERT INTO estado_entregable_config (codigo, nombre, descripcion, es_conforme, es_terminal, cuenta_avance, color_hex, orden) VALUES
('PENDIENTE',  'Pendiente',  'Entregable no iniciado',                FALSE, FALSE, 0,    '#6C757D', 1),
('EN_PROCESO', 'En Proceso', 'Entregable en desarrollo',              FALSE, FALSE, 50,   '#0D6EFD', 2),
('APROBADO',   'Aprobado',   'Entregable aprobado por el director',   TRUE,  FALSE, 100,  '#198754', 3),
('COMPLETADO', 'Completado', 'Entregable completado y verificado',    TRUE,  TRUE,  100,  '#28A745', 4),
('ATRASADO',   'Atrasado',   'Entregable con retraso',                FALSE, FALSE, 0,    '#DC3545', 5),
('RECHAZADO',  'Rechazado',  'Entregable rechazado, requiere corrección', FALSE, FALSE, 0, '#FFC107', 6);

-- =============================================================================
-- 6. TIPOS DE DOCUMENTO
-- =============================================================================

INSERT INTO tipo_documento_config (codigo, nombre, descripcion, require_pdf, orden) VALUES
('VIABILIZACION',          'Viabilización',           'Documento de viabilización del proyecto',           TRUE, 1),
('ACTA_CONSTITUCION',      'Acta de Constitución',    'Acta de constitución del proyecto',                 TRUE, 2),
('CRONOGRAMA',             'Cronograma',              'Cronograma de actividades del proyecto',            TRUE, 3),
('PLAN_COMUNICACIONES',    'Plan de Comunicaciones',  'Plan de comunicaciones del proyecto',               TRUE, 4),
('MATRIZ_RIESGOS_VIABILIDAD', 'Matriz de Riesgos (Viabilidad)', 'Matriz de riesgos para viabilización', TRUE, 5);

-- =============================================================================
-- 7. ESTRATEGIAS PETI
-- =============================================================================

INSERT INTO estrategia_peti_config (codigo, nombre, descripcion, vigencia_desde, vigencia_hasta, orden) VALUES
('TECNOLOGIAS_INFORMACION',            'Tecnologías de la Información',        'Estrategia de TI',           '2024', '2027', 1),
('TRANSFORMACION_DIGITAL',             'Transformación Digital',               'Estrategia de transformación digital', '2024', '2027', 2),
('CIUDADES_TERRITORIOS_INTELIGENTES',  'Ciudades y Territorios Inteligentes',  'Estrategia de territorios inteligentes', '2024', '2027', 3),
('GOBIERNO_DIGITAL',                   'Gobierno Digital',                     'Estrategia de gobierno digital', '2024', '2027', 4);

-- =============================================================================
-- 8. ESTADOS DE RIESGO
-- =============================================================================

INSERT INTO estado_riesgo_config (codigo, nombre, descripcion) VALUES
('IDENTIFICADO', 'Identificado', 'Riesgo recién identificado'),
('MITIGADO',     'Mitigado',     'Riesgo con acciones de mitigación en curso'),
('ACEPTADO',     'Aceptado',     'Riesgo aceptado por la dirección'),
('CERRADO',      'Cerrado',      'Riesgo cerrado o materializado');

-- =============================================================================
-- 9. MATRIZ DE RIESGO (5×5)
-- =============================================================================

INSERT INTO matriz_riesgo (probabilidad, impacto, nivel_riesgo, color, puntaje) VALUES
('UNO', 'UNO',   'BAJO',      '#28A745', 1),
('UNO', 'DOS',   'BAJO',      '#28A745', 2),
('UNO', 'TRES',  'MEDIO',     '#FFC107', 3),
('UNO', 'CUATRO','MEDIO',     '#FFC107', 4),
('UNO', 'CINCO', 'ALTO',      '#DC3545', 5),
('DOS', 'UNO',   'BAJO',      '#28A745', 2),
('DOS', 'DOS',   'MEDIO',     '#FFC107', 4),
('DOS', 'TRES',  'MEDIO',     '#FFC107', 6),
('DOS', 'CUATRO','ALTO',      '#DC3545', 8),
('DOS', 'CINCO', 'ALTO',      '#DC3545', 10),
('TRES','UNO',   'MEDIO',     '#FFC107', 3),
('TRES','DOS',   'MEDIO',     '#FFC107', 6),
('TRES','TRES',  'ALTO',      '#DC3545', 9),
('TRES','CUATRO','ALTO',      '#DC3545', 12),
('TRES','CINCO', 'MUY_ALTO',  '#DC3545', 15),
('CUATRO','UNO',   'MEDIO',   '#FFC107', 4),
('CUATRO','DOS',   'ALTO',    '#DC3545', 8),
('CUATRO','TRES',  'ALTO',    '#DC3545', 12),
('CUATRO','CUATRO','MUY_ALTO','#DC3545', 16),
('CUATRO','CINCO', 'MUY_ALTO','#DC3545', 20),
('CINCO','UNO',   'ALTO',      '#DC3545', 5),
('CINCO','DOS',   'ALTO',      '#DC3545', 10),
('CINCO','TRES',  'MUY_ALTO',  '#DC3545', 15),
('CINCO','CUATRO','MUY_ALTO',  '#DC3545', 20),
('CINCO','CINCO', 'MUY_ALTO',  '#DC3545', 25);

-- =============================================================================
-- 10. LISTAS PARAMÉTRICAS
-- =============================================================================

INSERT INTO lista_parametrica_config (lista_clave, item_codigo, item_nombre, orden, lista_nombre_campo, lista_descripcion, lista_tipo) VALUES
('DEPENDENCIA', 'SECRETARIA_GENERAL',       'Secretaría General',                1,  'Dependencias', 'Lista de dependencias institucionales', 'Lista'),
('DEPENDENCIA', 'SECRETARIA_TIC',           'Secretaría de TIC',                 2,  'Dependencias', 'Lista de dependencias institucionales', 'Lista'),
('DEPENDENCIA', 'SECRETARIA_FINANZAS',      'Secretaría de Finanzas',            3,  'Dependencias', 'Lista de dependencias institucionales', 'Lista'),
('DEPENDENCIA', 'SECRETARIA_PLANEACION',    'Secretaría de Planeación',          4,  'Dependencias', 'Lista de dependencias institucionales', 'Lista'),
('DEPENDENCIA', 'SECRETARIA_GOBIERNO',      'Secretaría de Gobierno',            5,  'Dependencias', 'Lista de dependencias institucionales', 'Lista'),
('DEPENDENCIA', 'SECRETARIA_SALUD',         'Secretaría de Salud',               6,  'Dependencias', 'Lista de dependencias institucionales', 'Lista'),
('DEPENDENCIA', 'SECRETARIA_EDUCACION',     'Secretaría de Educación',           7,  'Dependencias', 'Lista de dependencias institucionales', 'Lista'),
('DEPENDENCIA', 'SECRETARIA_INFRAESTRUCTURA','Secretaría de Infraestructura',     8,  'Dependencias', 'Lista de dependencias institucionales', 'Lista'),
('DEPENDENCIA', 'SECRETARIA_AMBIENTE',      'Secretaría de Ambiente',            9,  'Dependencias', 'Lista de dependencias institucionales', 'Lista'),
('DEPENDENCIA', 'FONDOS_CONVERGENCIA',      'Fondos de Convergencia',           10,  'Dependencias', 'Lista de dependencias institucionales', 'Lista'),
('ROL_USUARIO', 'ADMIN',              'Administrador',        1, 'Roles de Usuario', 'Roles disponibles en el sistema', 'Lista'),
('ROL_USUARIO', 'GESTOR_TIC',         'Gestor TIC',           2, 'Roles de Usuario', 'Roles disponibles en el sistema', 'Lista'),
('ROL_USUARIO', 'DIRECTOR_PROYECTO',  'Director de Proyecto', 3, 'Roles de Usuario', 'Roles disponibles en el sistema', 'Lista'),
('ROL_USUARIO', 'AUDITOR',            'Auditor',              4, 'Roles de Usuario', 'Roles disponibles en el sistema', 'Lista'),
('ROL_USUARIO', 'CONSULTA',           'Consulta',             5, 'Roles de Usuario', 'Roles disponibles en el sistema', 'Lista'),
('ROL_USUARIO', 'VISUALIZADOR',       'Visualizador',         6, 'Roles de Usuario', 'Roles disponibles en el sistema', 'Lista'),
('CARGO_ASIGNACION', 'DIRECTOR_PROYECTO',  'Director de Proyecto',  1, 'Cargos de Asignación', 'Cargos para asignación a proyectos', 'Lista'),
('CARGO_ASIGNACION', 'LIDER_TECNICO',      'Líder Técnico',         2, 'Cargos de Asignación', 'Cargos para asignación a proyectos', 'Lista'),
('CARGO_ASIGNACION', 'COLABORADOR',        'Colaborador',           3, 'Cargos de Asignación', 'Cargos para asignación a proyectos', 'Lista'),
('CARGO_ASIGNACION', 'REVISOR',            'Revisor',               4, 'Cargos de Asignación', 'Cargos para asignación a proyectos', 'Lista'),
('ROL_EQUIPO', 'DIRECTOR',              'Director',              1,  'Roles de Equipo', 'Roles del equipo del proyecto', 'Lista'),
('ROL_EQUIPO', 'LIDER_TECNICO',         'Líder Técnico',         2,  'Roles de Equipo', 'Roles del equipo del proyecto', 'Lista'),
('ROL_EQUIPO', 'ANALISTA',              'Analista',              3,  'Roles de Equipo', 'Roles del equipo del proyecto', 'Lista'),
('ROL_EQUIPO', 'DESARROLLADOR',         'Desarrollador',         4,  'Roles de Equipo', 'Roles del equipo del proyecto', 'Lista'),
('ROL_EQUIPO', 'DISEÑADOR',             'Diseñador',             5,  'Roles de Equipo', 'Roles del equipo del proyecto', 'Lista'),
('ROL_EQUIPO', 'PROBADOR',              'Probador',              6,  'Roles de Equipo', 'Roles del equipo del proyecto', 'Lista'),
('ROL_EQUIPO', 'DOCUMENTADOR',          'Documentador',          7,  'Roles de Equipo', 'Roles del equipo del proyecto', 'Lista'),
('ROL_EQUIPO', 'COORDINADOR',           'Coordinador',           8,  'Roles de Equipo', 'Roles del equipo del proyecto', 'Lista'),
('ROL_EQUIPO', 'RESPONSABLE_CALIDAD',   'Responsable de Calidad',9,  'Roles de Equipo', 'Roles del equipo del proyecto', 'Lista'),
('ROL_EQUIPO', 'RESPONSABLE_SEGURIDAD', 'Responsable de Seguridad',10,'Roles de Equipo', 'Roles del equipo del proyecto', 'Lista'),
('ROL_EQUIPO', 'STAKEHOLDER',           'Stakeholder',           11, 'Roles de Equipo', 'Roles del equipo del proyecto', 'Lista'),
('ROL_EQUIPO', 'OTRO',                  'Otro',                  12, 'Roles de Equipo', 'Roles del equipo del proyecto', 'Lista'),
('VIGENCIA_PETI', '2024', '2024', 1, 'Vigencia PETI', 'Años de vigencia PETI', 'Lista'),
('VIGENCIA_PETI', '2025', '2025', 2, 'Vigencia PETI', 'Años de vigencia PETI', 'Lista'),
('VIGENCIA_PETI', '2026', '2026', 3, 'Vigencia PETI', 'Años de vigencia PETI', 'Lista'),
('ESTRATEGIA_PETI', 'TECNOLOGIAS_INFORMACION',            'Tecnologías de la Información',        1, 'Estrategias PETI', 'Estrategias del PETI', 'Lista'),
('ESTRATEGIA_PETI', 'TRANSFORMACION_DIGITAL',             'Transformación Digital',               2, 'Estrategias PETI', 'Estrategias del PETI', 'Lista'),
('ESTRATEGIA_PETI', 'CIUDADES_TERRITORIOS_INTELIGENTES',  'Ciudades y Territorios Inteligentes',  3, 'Estrategias PETI', 'Estrategias del PETI', 'Lista'),
('ESTRATEGIA_PETI', 'GOBIERNO_DIGITAL',                   'Gobierno Digital',                     4, 'Estrategias PETI', 'Estrategias del PETI', 'Lista'),
('FURAG_PREGUNTAS', 'INFRAESTRUCTURA_DATOS',           'Infraestructura y Datos',              1, 'Preguntas FURAG', 'Preguntas FURAG', 'Lista'),
('FURAG_PREGUNTAS', 'INTEROPERABILIDAD',               'Interoperabilidad',                    2, 'Preguntas FURAG', 'Preguntas FURAG', 'Lista'),
('FURAG_PREGUNTAS', 'DIGITALIZACION_AUTOMATIZACION',   'Digitalización y Automatización',       3, 'Preguntas FURAG', 'Preguntas FURAG', 'Lista'),
('FURAG_PREGUNTAS', 'CONTRATACION_PUBLICA',            'Contratación Pública',                 4, 'Preguntas FURAG', 'Preguntas FURAG', 'Lista'),
('FURAG_PREGUNTAS', 'SERVICIOS_NUBE',                  'Servicios en Nube',                    5, 'Preguntas FURAG', 'Preguntas FURAG', 'Lista'),
('FURAG_PREGUNTAS', 'SANDBOX',                         'Sandbox',                              6, 'Preguntas FURAG', 'Preguntas FURAG', 'Lista'),
('FURAG_PREGUNTAS', 'TECNOLOGIAS_EMERGENTES',          'Tecnologías Emergentes',               7, 'Preguntas FURAG', 'Preguntas FURAG', 'Lista');

-- =============================================================================
-- 11. PARÁMETROS DEL SISTEMA
-- =============================================================================

INSERT INTO system_parameters (param_key, param_value, descripcion) VALUES
('advance_report_enabled',              'true',   'Habilitar módulo de reportes de avance'),
('advance_report_acceptance_threshold', '80',     'Umbral de aceptación para reportes de avance (%)'),
('advance_report_min_periodo',          '2024-01','Periodo mínimo para reportes de avance'),
('advance_report_max_periodo',          '2027-12','Periodo máximo para reportes de avance'),
('notifications.deadline-warning.days','7',       'Días de anticipación para alerta de vencimiento'),
('notifications.overdue-reminder.days','1',       'Días entre recordatorios de atraso'),
('notifications.deadline-warning.enabled','true', 'Habilitar alertas de vencimiento'),
('notif_prewizard_cargue_titulo',       'SE REALIZÓ EL CARGUE DE LOS DOCUMENTOS INICIALES DE PROYECTO', 'Título del correo de cargue de documentos iniciales'),
('notif_prewizard_cargue_intro',        'Se cargaron los documentos iniciales requeridos para continuar con el proceso del proyecto.', 'Intro del correo de cargue de documentos iniciales'),
('notif_prewizard_aprobado_titulo',     'SE APROBARON LOS DOCUMENTOS INICIALES DE PROYECTO', 'Título del correo de aprobación de documentos iniciales'),
('notif_prewizard_aprobado_intro',      'Los documentos iniciales del proyecto fueron verificados y aprobados por el gestor.', 'Intro del correo de aprobación'),
('notif_prewizard_devuelto_titulo',     'SE DEVOLVIERON DOCUMENTOS INICIALES DE PROYECTO PARA CORRECCIÓN', 'Título del correo de devolución de documentos iniciales'),
('notif_prewizard_devuelto_intro',      'Los documentos iniciales del proyecto fueron devueltos con observaciones para su corrección.', 'Intro del correo de devolución'),
('notif_prewizard_estado_etiqueta',     'Estado de los documentos iniciales del proyecto:', 'Etiqueta de estados en correos pre-wizard'),
('notif_prewizard_enlace_texto',        'Puede acceder directamente a través del siguiente enlace:', 'Texto del enlace en correos pre-wizard')
ON CONFLICT (param_key) DO UPDATE SET
    param_value  = EXCLUDED.param_value,
    descripcion  = EXCLUDED.descripcion;

-- =============================================================================
-- 12. CONFIGURACIÓN DE REPORTES
-- =============================================================================

INSERT INTO reporte_config (id, nombre, descripcion, orden) VALUES
('ESTADO_PROYECTO_ESPECIFICO', 'Estado de Proyecto Específico', 'Reporte del estado detallado de un proyecto', 1),
('ESTADO_TODOS_PROYECTOS',     'Estado de Todos los Proyectos', 'Reporte consolidado del estado de todos los proyectos', 2),
('FURAG',                      'FURAG',                         'Reporte de evaluación FURAG', 3),
('PLAN_COMUNICACIONES',        'Plan de Comunicaciones',        'Reporte del plan de comunicaciones', 4),
('RIESGOS_VERIFICACION',       'Verificación de Riesgos',       'Reporte de verificación de riesgos', 5),
('ANALITICA_PORTAFOLIO',       'Analítica del Portafolio',      'Reporte de analítica del portafolio', 6),
('PROYECTOS_CON_RETRASOS',     'Proyectos con Retrasos',        'Reporte de proyectos con retrasos', 7),
('ACTA_CIERRE',                'Acta de Cierre',                'Reporte de acta de cierre de proyecto', 8),
('MATRIZ_RIESGOS',             'Matriz de Riesgos',             'Reporte de matriz de riesgos', 9);

-- =============================================================================
-- 13. CATÁLOGO DE EVENTOS DE NOTIFICACIÓN
-- =============================================================================

INSERT INTO notification_event_catalog (code, name, description, category, default_enabled, requires_project_context) VALUES
('PROJECT_INITIAL_REGISTERED',        'Proyecto Registrado',                 'Notificación cuando se registra un nuevo proyecto',                 'PROYECTO',    TRUE,  TRUE),
('PROJECT_INITIAL_COMPLETED',         'Información Inicial Completada',      'Notificación cuando el director completa la información inicial',    'PROYECTO',    TRUE,  TRUE),
('PROJECT_UPDATED',                   'Proyecto Actualizado',                'Notificación cuando se actualizan datos del proyecto',               'PROYECTO',    TRUE,  TRUE),
('PROJECT_DELAYED',                   'Proyecto con Retrasos',               'Notificación cuando el proyecto presenta entregables vencidos',      'PROYECTO',    TRUE,  TRUE),
('PROJECT_CLOSED',                    'Proyecto Cerrado',                    'Notificación cuando el proyecto es cerrado',                         'PROYECTO',    TRUE,  TRUE),
('PROJECT_ASSIGNMENT_CREATED',        'Asignación en Proyecto',              'Notificación cuando se asigna un usuario al proyecto',               'PROYECTO',    TRUE,  TRUE),
('DELIVERABLE_EVIDENCE_UPLOADED',     'Evidencia Cargada',                   'Notificación cuando se carga evidencia en un entregable',            'ENTREGABLE',  TRUE,  TRUE),
('DELIVERABLE_APPROVED',              'Entregable Aprobado',                 'Notificación cuando se aprueba un entregable',                       'ENTREGABLE',  TRUE,  TRUE),
('DELIVERABLE_REJECTED',              'Entregable Rechazado',                'Notificación cuando se rechaza un entregable',                       'ENTREGABLE',  TRUE,  TRUE),
('OBSERVATION_SUBSANATED',            'Observación Subsanada',               'Notificación cuando se subsana una observación',                     'ENTREGABLE',  TRUE,  TRUE),
('PROJECT_BENEFIT_IMPACT_REQUIRED',   'Beneficio e Impacto Requerido',       'Notificación cuando se requiere diligenciar beneficio e impacto',     'BENEFICIO',   TRUE,  TRUE),
('PROJECT_BENEFIT_IMPACT_SUBMITTED',  'Beneficio e Impacto Diligenciado',    'Notificación cuando el director registra beneficio e impacto',        'BENEFICIO',   TRUE,  TRUE),
('PROJECT_BENEFIT_IMPACT_RESUBMITTED','Beneficio e Impacto Corregido',       'Notificación cuando el director reenvía beneficio e impacto',         'BENEFICIO',   TRUE,  TRUE),
('PROJECT_BENEFIT_IMPACT_REVIEWED',   'Beneficio e Impacto Revisado',        'Notificación cuando el gestor revisa beneficio e impacto',            'BENEFICIO',   TRUE,  TRUE),
('RISK_CREATED',                      'Riesgo Creado',                       'Notificación cuando se crea un riesgo',                               'RIESGO',      TRUE,  TRUE),
('RISK_UPDATED',                      'Riesgo Actualizado',                  'Notificación cuando se modifica un riesgo',                           'RIESGO',      TRUE,  TRUE),
('RISK_TREATED',                      'Riesgo Tratado',                      'Notificación cuando un riesgo pasa a estado tratado',                 'RIESGO',      TRUE,  TRUE),
('CLOSURE_REQUESTED',                 'Solicitud de Cierre',                 'Notificación cuando se solicita cierre de proyecto',                  'CIERRE',      TRUE,  TRUE),
('CLOSURE_APPROVED',                  'Cierre Aprobado',                     'Notificación cuando se aprueba el cierre',                            'CIERRE',      TRUE,  TRUE),
('CLOSURE_REJECTED',                  'Cierre Rechazado',                    'Notificación cuando se rechaza el cierre',                            'CIERRE',      TRUE,  TRUE),
('ENTREGABLE_FECHA_CAMBIADA',         'Fecha de Entrega Cambiada',           'Notificación cuando cambia la fecha de un entregable',                'ENTREGABLE',  TRUE,  TRUE),
('ENTREGABLE_DEADLINE_WARNING',       'Vencimiento Próximo',                 'Alerta antes del vencimiento de un entregable',                       'ENTREGABLE',  TRUE,  TRUE),
('ENTREGABLE_OVERDUE_REMINDER',       'Entregable Vencido',                  'Recordatorio periódico de entregable vencido',                        'ENTREGABLE',  TRUE,  TRUE),
('PROJECT_DOCUMENT_UPLOADED',         'Documento Cargado',                   'Notificación cuando se carga un documento del proyecto',              'DOCUMENTO',   TRUE,  TRUE),
('PROJECT_DOCUMENT_DELETED',          'Documento Eliminado',                 'Notificación cuando se elimina un documento del proyecto',            'DOCUMENTO',   TRUE,  TRUE),
('PROJECT_DIRECTOR_ALERT',            'Seguimiento de Avance',               'Alerta de seguimiento de avance para el director',                    'SISTEMA',     TRUE,  TRUE),
('SECURITY_ROLE_UPDATED',             'Rol de Seguridad Actualizado',        'Notificación cuando se modifica un rol de seguridad',                 'SEGURIDAD',   TRUE,  FALSE),
('SECURITY_USER_UPDATED',             'Usuario de Seguridad Actualizado',    'Notificación cuando se actualizan datos de un usuario de seguridad',  'SEGURIDAD',   TRUE,  FALSE),
('VIABILIDAD_UPLOADED',               'Documentos Pre-Wizard Cargados',      'Notificación unificada con el estado de los 3 documentos pre-wizard', 'DOCUMENTO',   TRUE,  TRUE),
('VIABILIDAD_APPROVED',               'Documentos Pre-Wizard Aprobados',     'Notificación unificada: documentos pre-wizard aprobados',             'DOCUMENTO',   TRUE,  TRUE),
('VIABILIDAD_RETURNED',               'Documentos Pre-Wizard Devueltos',     'Notificación unificada: documentos pre-wizard devueltos',             'DOCUMENTO',   TRUE,  TRUE),
('ACTA_CONSTITUCION_REMINDER',        'Recordatorio Acta de Constitución',   'Recordatorio de plazo para cargar el acta de constitución',           'PROYECTO',    TRUE,  TRUE),
('ADVANCE_REPORT_DUE_NOTIFICATION',   'Informe de Avance Pendiente',         'Notificación de informe de avance pendiente o vencido',               'PROYECTO',    TRUE,  TRUE),
('ADVANCE_REPORT_UPLOADED',           'Informe de Avance Cargado',           'Notificación cuando el director carga el informe de avance',          'PROYECTO',    TRUE,  TRUE),
('ADVANCE_REPORT_VERIFIED',           'Informe de Avance Verificado',        'Notificación cuando el gestor verifica el informe de avance',         'PROYECTO',    TRUE,  TRUE),
('ADVANCE_REPORT_RETURNED',           'Informe de Avance Devuelto',          'Notificación cuando el gestor devuelve el informe de avance',         'PROYECTO',    TRUE,  TRUE),
('RISK_DELETED',                      'Riesgo Eliminado',                    'Notificación cuando se elimina un riesgo',                            'RIESGO',      TRUE,  TRUE),
('EVIDENCE_VERSION_REVERTED',         'Versión de Evidencia Revertida',      'Notificación cuando el gestor revierte una versión de evidencia',     'ENTREGABLE',  TRUE,  TRUE)
ON CONFLICT (code) DO UPDATE SET
    name                     = EXCLUDED.name,
    description              = EXCLUDED.description,
    category                 = EXCLUDED.category,
    default_enabled          = EXCLUDED.default_enabled,
    requires_project_context = EXCLUDED.requires_project_context,
    active                   = TRUE;

-- =============================================================================
-- 14. PLANTILLAS DE NOTIFICACIÓN
-- =============================================================================

INSERT INTO notification_template (event_code, enabled, subject_template, body_template, target_roles, updated_by, updated_at) VALUES
('PROJECT_INITIAL_REGISTERED', TRUE,
 'Nuevo proyecto registrado: {{projectName}}',
 'El proyecto "{{projectName}}" ({{projectId}}) fue registrado por {{actorUsername}} y actualmente se encuentra en estado "{{state}}". Por favor, diríjase al proyecto para que el director pueda completar la información inicial.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos"]', 'seeder', CURRENT_TIMESTAMP),

('PROJECT_INITIAL_COMPLETED', TRUE,
 'Informacion inicial completada: {{projectName}}',
 'El director {{actorUsername}} completó la información inicial del proyecto "{{projectName}}" ({{projectId}}). El proyecto se encuentra ahora en estado "{{state}}" y puede avanzar en su ejecución.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos"]', 'seeder', CURRENT_TIMESTAMP),

('PROJECT_UPDATED', TRUE,
 'Proyecto actualizado: {{projectName}}',
 'Se actualizaron los datos del proyecto "{{projectName}}" ({{projectId}}) por {{actorUsername}}. Por favor, revise los cambios en la plataforma para verificar que la información esté al día.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('PROJECT_DELAYED', TRUE,
 'Proyecto con retrasos: {{projectName}}',
 'El proyecto "{{projectName}}" ({{projectId}}) presenta {{overdueDeliverables}} entregable(s) con fecha de entrega vencida. Es necesario revisar el avance y tomar acciones para recuperar el cronograma.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('PROJECT_CLOSED', TRUE,
 'Proyecto cerrado: {{projectName}}',
 'El proyecto "{{projectName}}" ({{projectId}}) ha sido cerrado exitosamente. El proyecto pasó al estado "{{state}}" y ya no acepta modificaciones.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('PROJECT_ASSIGNMENT_CREATED', TRUE,
 'Nueva asignacion en proyecto: {{projectName}}',
 'El usuario {{assignedUsername}} fue asignado al proyecto "{{projectName}}" ({{projectId}}) con el cargo de "{{assignmentRole}}". Ya puede acceder a la información del proyecto en la plataforma.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('DELIVERABLE_EVIDENCE_UPLOADED', TRUE,
 'Evidencia cargada: {{deliverableName}} - {{projectName}}',
 'Se cargó una nueva evidencia en el entregable "{{deliverableName}}" del proyecto "{{projectName}}" ({{projectId}}). Por favor, revise la evidencia adjunta en la sección de avance del entregable.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('DELIVERABLE_APPROVED', TRUE,
 'Entregable aprobado: {{deliverableName}} - {{projectName}}',
 'El entregable "{{deliverableName}}" del proyecto "{{projectName}}" ({{projectId}}) fue aprobado por {{actorUsername}}. El avance del proyecto se ha actualizado automáticamente.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('DELIVERABLE_REJECTED', TRUE,
 'Entregable rechazado: {{deliverableName}} - {{projectName}}',
 'El entregable "{{deliverableName}}" del proyecto "{{projectName}}" ({{projectId}}) fue rechazado por {{actorUsername}}. Observación: "{{observation}}". Por favor, subsane la observación y vuelva a enviar la evidencia.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('OBSERVATION_SUBSANATED', TRUE,
 'Observacion subsanada: {{deliverableName}} - {{projectName}}',
 'La observación #{{observationId}} del entregable "{{deliverableName}}" del proyecto "{{projectName}}" ({{projectId}}) fue subsanada por {{actorUsername}}. La evidencia ha sido corregida y está lista para nueva revisión.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('PROJECT_BENEFIT_IMPACT_REQUIRED', TRUE,
 'Beneficio e impacto requerido: {{projectName}}',
 'El proyecto "{{projectName}}" ({{projectId}}) alcanzó el 100% de entregables aprobados. El Director debe diligenciar la información de beneficio e impacto para continuar con el proceso de cierre.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('PROJECT_BENEFIT_IMPACT_SUBMITTED', TRUE,
 'Beneficio e impacto diligenciado: {{projectName}}',
 'El director {{actorUsername}} registró la información de beneficio e impacto del proyecto "{{projectName}}" ({{projectId}}). La información está disponible para revisión del gestor.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos"]', 'seeder', CURRENT_TIMESTAMP),

('PROJECT_BENEFIT_IMPACT_RESUBMITTED', TRUE,
 'Beneficio e impacto corregido: {{projectName}}',
 'El director {{actorUsername}} corrigió y reenvió la información de beneficio e impacto del proyecto "{{projectName}}" ({{projectId}}) después de una observación. Revise la información corregida.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos"]', 'seeder', CURRENT_TIMESTAMP),

('PROJECT_BENEFIT_IMPACT_REVIEWED', TRUE,
 'Beneficio e impacto {{aprobado}}: {{projectName}}',
 'La información de beneficio e impacto del proyecto "{{projectName}}" ({{projectId}}) fue {{aprobado}} por el gestor. {{observaciones}}
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('RISK_CREATED', TRUE,
 'Riesgo creado: {{riskCode}} - {{projectName}}',
 'Se registró el riesgo {{riskCode}} en el proyecto "{{projectName}}" ({{projectId}}) con nivel de impacto "{{riskLevel}}". Por favor, revise los detalles del riesgo en la sección de gestión de riesgos del proyecto.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('RISK_UPDATED', TRUE,
 'Riesgo actualizado: {{riskCode}} - {{projectName}}',
 'El riesgo {{riskCode}} del proyecto "{{projectName}}" ({{projectId}}) fue modificado por {{actorUsername}}. Verifique los cambios realizados en la sección de gestión de riesgos.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('RISK_TREATED', TRUE,
 'Riesgo tratado: {{riskCode}} - {{projectName}}',
 'El riesgo {{riskCode}} del proyecto "{{projectName}}" ({{projectId}}) pasó a estado "Tratado". Se aplicaron las acciones de mitigación o tratamiento correspondientes.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('CLOSURE_REQUESTED', TRUE,
 'Solicitud de cierre: {{projectName}}',
 'El usuario {{requester}} solicitó formalmente el cierre del proyecto "{{projectName}}" ({{projectId}}). Como gestor, proceda con las validaciones correspondientes y apruebe o rechace la solicitud.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos"]', 'seeder', CURRENT_TIMESTAMP),

('CLOSURE_APPROVED', TRUE,
 'Cierre aprobado: {{projectName}}',
 'La solicitud de cierre del proyecto "{{projectName}}" ({{projectId}}) fue aprobada por {{approver}}. El proyecto está formalmente cerrado y no aceptará más modificaciones.',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('CLOSURE_REJECTED', TRUE,
 'Cierre rechazado: {{projectName}}',
 'La solicitud de cierre del proyecto "{{projectName}}" ({{projectId}}) fue rechazada por {{rejector}}. Motivo: "{{observaciones}}". Por favor, corrija las observaciones y envíe una nueva solicitud de cierre.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('ENTREGABLE_FECHA_CAMBIADA', TRUE,
 'Fecha de entrega cambiada: {{entregableNombre}}',
 'La fecha del entregable "{{entregableNombre}}" fue cambiada. Fecha anterior: {{fechaAnterior}}. Nueva fecha: {{fechaNueva}}. Justificación: "{{justificacion}}". Verifique el impacto en el cronograma del proyecto.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('ENTREGABLE_DEADLINE_WARNING', TRUE,
 'Vencimiento proximo: {{entregableNombre}} - {{diasRestantes}} dias',
 'El entregable "{{entregableNombre}}" del proyecto "{{projectName}}" vence en {{diasRestantes}} días (fecha limite: {{fechaLimite}}). Por favor, asegúrese de gestionar la evidencia correspondiente antes de la fecha de corte.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('ENTREGABLE_OVERDUE_REMINDER', TRUE,
 'Entregable vencido: {{entregableNombre}} - {{diasVencido}} dias de atraso',
 'El entregable "{{entregableNombre}}" del proyecto "{{projectName}}" se encuentra vencido desde hace {{diasVencido}} días (fecha limite: {{fechaLimite}}). Este es un recordatorio periódico. Por favor, gestione la entrega pendiente lo antes posible.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('PROJECT_DOCUMENT_UPLOADED', TRUE,
 'Documento cargado: {{documentType}} - {{projectName}}',
 'Se cargó el documento "{{documentType}}" ({{documentName}}) en el proyecto "{{projectName}}" ({{projectId}}) por {{actorUsername}}. Por favor, revise el documento en la sección de documentos del proyecto.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('PROJECT_DOCUMENT_DELETED', TRUE,
 'Documento eliminado: {{documentType}} - {{projectName}}',
 'El documento "{{documentType}}" ({{documentName}}) del proyecto "{{projectName}}" ({{projectId}}) fue eliminado por {{actorUsername}}. Verifique si es necesario restaurarlo o reemplazarlo.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('PROJECT_DIRECTOR_ALERT', TRUE,
 'Seguimiento de avance: {{projectName}} ({{projectId}})',
 'Proyecto: {{projectName}}
Estado: {{projectState}}
Avance total: {{avanceTotal}}
Riesgos pendientes ({{pendingRisksCount}}):
{{pendingRisksDetail}}
Entregables pendientes ({{pendingDeliverablesCount}}):
{{pendingDeliverablesDetail}}
Entregables vencidos ({{overdueCount}}):
{{overdueDetail}}
Documentos faltantes:
{{missingDocumentsDetail}}
Enviado por: {{sentBy}}
Fecha: {{sentAt}}
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('SECURITY_ROLE_UPDATED', TRUE,
 'Configuracion de seguridad actualizada',
 'Se modificó el rol "{{roleCode}}" en la configuración de seguridad del sistema. Si tiene permisos afectados, verifique que su acceso siga funcionando correctamente.',
 '["admin"]', 'seeder', CURRENT_TIMESTAMP),

('SECURITY_USER_UPDATED', TRUE,
 'Usuario actualizado',
 'Se actualizaron datos de seguridad para {{username}}.',
 '["admin"]', 'seeder', CURRENT_TIMESTAMP),

('VIABILIDAD_UPLOADED', TRUE,
 '{{mensajeTitulo}}: {{projectName}}',
 '{{mensajeIntro}}
Proyecto: "{{projectName}}" ({{projectId}}).

{{estadoEtiqueta}}
{{documentStatuses}}

{{enlaceTexto}}
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('VIABILIDAD_APPROVED', TRUE,
 '{{mensajeTitulo}}: {{projectName}}',
 '{{mensajeIntro}}
Proyecto: "{{projectName}}" ({{projectId}}). Revisado por {{actorUsername}}.

{{estadoEtiqueta}}
{{documentStatuses}}

{{enlaceTexto}}
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('VIABILIDAD_RETURNED', TRUE,
 '{{mensajeTitulo}}: {{projectName}}',
 '{{mensajeIntro}}
Proyecto: "{{projectName}}" ({{projectId}}).

{{estadoEtiqueta}}
{{documentStatuses}}

Observaciones: {{observaciones}}

{{enlaceTexto}}
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('ACTA_CONSTITUCION_REMINDER', TRUE,
 'Recordatorio acta de constitución: {{projectName}}',
 'El proyecto "{{projectName}}" ({{projectId}}) tiene el acta de constitución pendiente. Quedan {{diasRestantes}} día(s) para cargarla.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('ADVANCE_REPORT_DUE_NOTIFICATION', TRUE,
 'Informe de avance pendiente: {{projectName}}',
 'El proyecto "{{projectName}}" ({{projectId}}) tiene un informe de avance pendiente. Fecha límite: {{dueDate}}.
{{message}}
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('ADVANCE_REPORT_UPLOADED', TRUE,
 'Informe de avance cargado: {{projectName}}',
 'Se cargó el informe de avance del periodo {{periodo}} para el proyecto "{{projectName}}" ({{projectId}}).
Archivo: {{fileName}}
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('ADVANCE_REPORT_VERIFIED', TRUE,
 'Informe de avance verificado: {{projectName}}',
 'El informe de avance del periodo {{periodo}} del proyecto "{{projectName}}" ({{projectId}}) fue verificado y aprobado.
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('ADVANCE_REPORT_RETURNED', TRUE,
 'Informe de avance devuelto: {{projectName}}',
 'El informe de avance del periodo {{periodo}} del proyecto "{{projectName}}" ({{projectId}}) fue devuelto con observaciones:
{{observaciones}}
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('RISK_DELETED', TRUE,
 'Riesgo eliminado en {{projectName}}',
 'Se eliminó el riesgo {{riskCode}} del proyecto "{{projectName}}" ({{projectId}}).
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP),

('EVIDENCE_VERSION_REVERTED', TRUE,
 'Evidencia revertida en {{projectName}}',
 'Se revirtió la evidencia del entregable "{{deliverableName}}" del proyecto "{{projectName}}" ({{projectId}}).
Motivo: {{motivo}}
Puede acceder directamente a través del siguiente enlace:
{{targetUrl}}',
 '["admin","gestor_tic","gestor_proyectos","director_proyecto"]', 'seeder', CURRENT_TIMESTAMP)
ON CONFLICT (event_code) DO UPDATE SET
    enabled          = EXCLUDED.enabled,
    subject_template = EXCLUDED.subject_template,
    body_template    = EXCLUDED.body_template,
    target_roles     = EXCLUDED.target_roles,
    updated_by       = EXCLUDED.updated_by,
    updated_at       = EXCLUDED.updated_at;


-- =============================================================================
-- 15. PLANTILLAS DE CIERRE
-- =============================================================================

INSERT INTO project_closure_template (codigo_proceso, version_num, nombre_documento, activo, template_json, created_by) VALUES
('A-GT-FR-004', 1, 'Acta de Cierre del Proyecto', FALSE,
 '{"secciones":[{"nombre":"Información General","campos":["proyecto_id","nombre","dependencia","director"]},{"nombre":"Resumen Ejecutivo","campos":["resumen"]},{"nombre":"Resultados","campos":["avance_final","estado_final"]}]}'::jsonb,
 'system'),
('A-GT-FR-004', 2, 'Acta de Cierre del Proyecto', FALSE,
 '{"secciones":[{"nombre":"Información General","campos":["proyecto_id","nombre","dependencia","director","fecha_inicio","fecha_cierre"]},{"nombre":"Resumen Ejecutivo","campos":["resumen","objetivos_cumplidos"]},{"nombre":"Resultados","campos":["avance_final","estado_final","eficacia"]},{"nombre":"Lecciones Aprendidas","campos":["lecciones"]}]}'::jsonb,
 'system'),
('A-GT-FR-004', 3, 'Acta de Cierre del Proyecto', TRUE,
 '{"secciones":[{"nombre":"Información General","campos":["proyecto_id","nombre","dependencia","director","fecha_inicio","fecha_cierre","presupuesto"]},{"nombre":"Resumen Ejecutivo","campos":["resumen","objetivos_cumplidos","alcance"]},{"nombre":"Resultados","campos":["avance_final","estado_final","eficacia","progreso_programado","progreso_ejecutado"]},{"nombre":"Lecciones Aprendidas","campos":["lecciones","recomendaciones"]},{"nombre":"Cierre Administrativo","campos":["documentos_entregados","pendientes"]}]}'::jsonb,
 'system');

-- =============================================================================
-- 16. PREGUNTAS DINÁMICAS DE CIERRE
-- =============================================================================

INSERT INTO project_closure_question (texto, tipo_respuesta, activo, orden, created_by) VALUES
('¿Se cumplieron todos los objetivos del proyecto?',          'si_no',          TRUE, 1,  'system'),
('¿Qué lecciones aprendidas destacaría del proyecto?',        'texto_libre',    TRUE, 2,  'system'),
('¿Existieron desviaciones significativas en el presupuesto?','si_no_detallado',TRUE, 3,  'system'),
('¿Qué recomendaciones haría para futuros proyectos similares?','texto_libre',  TRUE, 4,  'system'),
('¿Todos los entregables fueron aprobados?',                 'si_no',          TRUE, 5,  'system'),
('¿El proyecto generó los impactos esperados?',              'si_no_detallado',TRUE, 6,  'system');

-- =============================================================================
-- FIN DE DATOS SEMILLA
-- =============================================================================
