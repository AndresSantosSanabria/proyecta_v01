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
('SIDEBAR:VER_CONFIGURACION',            'Ver Menú Configuración',              'Visible en menú de configuración');

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

-- Visualizador: mínimo acceso
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.codigo = 'visualizador' AND p.codigo IN ('DASHBOARD:VER', 'PROYECTO:VER', 'ENTREGABLE:VER');

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
('notifications.deadline-warning.enabled','true', 'Habilitar alertas de vencimiento');

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
('PROJECT_CREATED',              'Proyecto Creado',              'Notificación cuando se crea un nuevo proyecto',              'PROYECTO',  TRUE,  TRUE),
('PROJECT_DIRECTOR_ASSIGNED',    'Director Asignado',            'Notificación cuando se asigna director a un proyecto',       'PROYECTO',  TRUE,  TRUE),
('PROJECT_STATUS_CHANGED',       'Estado de Proyecto Cambiado',  'Notificación cuando cambia el estado del proyecto',          'PROYECTO',  TRUE,  TRUE),
('DELIVERABLE_APPROVED',         'Entregable Aprobado',          'Notificación cuando se aprueba un entregable',               'ENTREGABLE', TRUE,  TRUE),
('DELIVERABLE_REJECTED',         'Entregable Rechazado',         'Notificación cuando se rechaza un entregable',               'ENTREGABLE', TRUE,  TRUE),
('DELIVERABLE_DEADLINE_WARNING', 'Alerta de Vencimiento',        'Alerta antes del vencimiento de un entregable',              'ENTREGABLE', TRUE,  TRUE),
('DELIVERABLE_OVERDUE',          'Entregable Atrasado',          'Notificación cuando un entregable está atrasado',            'ENTREGABLE', TRUE,  TRUE),
('EVIDENCE_UPLOADED',            'Evidencia Subida',             'Notificación cuando se sube una evidencia',                  'EVIDENCIA',  TRUE,  TRUE),
('EVIDENCE_APPROVED',            'Evidencia Aprobada',           'Notificación cuando se aprueba una evidencia',               'EVIDENCIA',  TRUE,  TRUE),
('EVIDENCE_OBSERVATION',         'Observación en Evidencia',     'Notificación cuando se agrega observación a evidencia',      'EVIDENCIA',  TRUE,  TRUE),
('DOCUMENT_UPLOADED',            'Documento Subido',             'Notificación cuando se sube un documento',                   'DOCUMENTO',  TRUE,  TRUE),
('CLOSURE_REQUESTED',            'Cierre Solicitado',            'Notificación cuando se solicita cierre de proyecto',         'CIERRE',     TRUE,  TRUE),
('CLOSURE_APPROVED',             'Cierre Aprobado',              'Notificación cuando se aprueba el cierre',                   'CIERRE',     TRUE,  TRUE),
('CLOSURE_REJECTED',             'Cierre Rechazado',             'Notificación cuando se rechaza el cierre',                   'CIERRE',     TRUE,  TRUE),
('BENEFIT_IMPACT_REQUESTED',     'Beneficio/Impacto Solicitado', 'Notificación cuando se solicita información de beneficio',   'BENEFICIO',  TRUE,  TRUE),
('BENEFIT_IMPACT_REVIEWED',      'Beneficio/Impacto Revisado',   'Notificación cuando se revisa el beneficio/impacto',         'BENEFICIO',  TRUE,  TRUE),
('DIRECTOR_ALERT',               'Alerta del Director',          'Alerta general para el director de proyecto',                'SISTEMA',    TRUE,  TRUE),
('PROJECT_DELAY_INTERNAL',       'Retraso Interno',              'Notificación interna de retraso de proyecto',                'SISTEMA',    TRUE,  FALSE);

-- =============================================================================
-- 14. PLANTILLAS DE NOTIFICACIÓN
-- =============================================================================

INSERT INTO notification_template (event_code, enabled, subject_template, body_template, target_roles) VALUES
('PROJECT_CREATED',            TRUE, 'Nuevo Proyecto: {{projectName}}',       'Se ha creado el proyecto {{projectName}} con código {{projectId}}.', '["admin","gestor_tic"]'),
('PROJECT_DIRECTOR_ASSIGNED',  TRUE, 'Director Asignado: {{projectName}}',    'Ha sido asignado como director del proyecto {{projectName}}.', '["director_proyecto"]'),
('PROJECT_STATUS_CHANGED',     TRUE, 'Cambio de Estado: {{projectName}}',     'El proyecto {{projectName}} ha cambiado de estado a {{newStatus}}.', '["admin","gestor_tic","director_proyecto"]'),
('DELIVERABLE_APPROVED',       TRUE, 'Entregable Aprobado: {{deliverableName}}','El entregable {{deliverableName}} ha sido aprobado.', '["director_proyecto","gestor_tic"]'),
('DELIVERABLE_REJECTED',       TRUE, 'Entregable Rechazado: {{deliverableName}}','El entregable {{deliverableName}} ha sido rechazado. Motivo: {{reason}}', '["director_proyecto","gestor_tic"]'),
('DELIVERABLE_DEADLINE_WARNING',TRUE,'Alerta de Vencimiento: {{deliverableName}}','El entregable {{deliverableName}} vence en {{daysLeft}} días.', '["director_proyecto","gestor_tic"]'),
('DELIVERABLE_OVERDUE',        TRUE, 'Entregable Atrasado: {{deliverableName}}','El entregable {{deliverableName}} tiene {{daysOverdue}} días de retraso.', '["director_proyecto","gestor_tic","admin"]'),
('EVIDENCE_UPLOADED',          TRUE, 'Evidencia Subida: {{deliverableName}}', 'Se ha subido una nueva evidencia para el entregable {{deliverableName}}.', '["director_proyecto","gestor_tic"]'),
('EVIDENCE_APPROVED',          TRUE, 'Evidencia Aprobada: {{deliverableName}}','La evidencia del entregable {{deliverableName}} ha sido aprobada.', '["director_proyecto","gestor_tic"]'),
('EVIDENCE_OBSERVATION',       TRUE, 'Observación en Evidencia: {{deliverableName}}','Se ha agregado una observación a la evidencia del entregable {{deliverableName}}.', '["director_proyecto","gestor_tic"]'),
('DOCUMENT_UPLOADED',          TRUE, 'Documento Subido: {{projectName}}',     'Se ha subido el documento {{docType}} al proyecto {{projectName}}.', '["director_proyecto","gestor_tic"]'),
('CLOSURE_REQUESTED',          TRUE, 'Cierre Solicitado: {{projectName}}',    'Se ha solicitado el cierre del proyecto {{projectName}}.', '["admin","gestor_tic"]'),
('CLOSURE_APPROVED',           TRUE, 'Cierre Aprobado: {{projectName}}',      'El cierre del proyecto {{projectName}} ha sido aprobado.', '["director_proyecto","gestor_tic"]'),
('CLOSURE_REJECTED',           TRUE, 'Cierre Rechazado: {{projectName}}',     'El cierre del proyecto {{projectName}} ha sido rechazado. Motivo: {{reason}}', '["director_proyecto","gestor_tic"]'),
('BENEFIT_IMPACT_REQUESTED',   TRUE, 'Beneficio/Impacto Solicitado: {{projectName}}','Se ha solicitado información de beneficio e impacto para el proyecto {{projectName}}.', '["director_proyecto"]'),
('BENEFIT_IMPACT_REVIEWED',    TRUE, 'Beneficio/Impacto Revisado: {{projectName}}','La información de beneficio e impacto del proyecto {{projectName}} ha sido revisada.', '["director_proyecto","gestor_tic"]'),
('DIRECTOR_ALERT',             TRUE, 'Alerta: {{projectName}}',               '{{alertMessage}}', '["director_proyecto"]'),
('PROJECT_DELAY_INTERNAL',     TRUE, 'Retraso Interno: {{projectName}}',      'El proyecto {{projectName}} presenta un retraso de {{delayDays}} días.', '["admin","gestor_tic"]');

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
