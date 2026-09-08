package com.proyecta.api_gestion.service.seed;

import com.proyecta.api_gestion.model.SystemParameter;
import com.proyecta.api_gestion.repository.SystemParameterRepository;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Seeder especializado para parametros del sistema.
 *
 * Responsabilidad: Inicializar parametros de configuracion global (SRP).
 */
@Service
public class SystemParameterSeeder {

    private static final Logger logger = LoggerFactory.getLogger(SystemParameterSeeder.class);
    private final SystemParameterRepository parameterRepository;

    public SystemParameterSeeder(SystemParameterRepository parameterRepository) {
        this.parameterRepository = parameterRepository;
    }

    public void seedSystemParameters() {
        logger.info("Cargando parametros del sistema...");

        crearParametroSiNoExiste(
            SystemParameterKeys.DASHBOARD_VENTANA_VENCIMIENTO_DIAS,
            "8",
            "Dias ventana de vencimiento para alertas de proyectos"
        );

        crearParametroSiNoExiste(
            SystemParameterKeys.DASHBOARD_AVANCE_CRITICO_PORCENTAJE,
            "50",
            "Porcentaje minimo de avance para considerar proyecto en riesgo"
        );

        crearParametroSiNoExiste(
            SystemParameterKeys.DASHBOARD_MAX_PROYECTOS_ACTIVOS,
            "20",
            "Maximo numero de proyectos activos simultaneos"
        );

        crearParametroSiNoExiste(
            SystemParameterKeys.SEGURIDAD_CARGOS_ASIGNACION,
            "DIRECTOR_PROYECTO,LIDER_TECNICO,ANALISTA,APOYO",
            "Cargos disponibles para asignar usuarios a proyectos"
        );

        crearParametroSiNoExiste(
            SystemParameterKeys.SEGURIDAD_ROLE_ALIASES,
            "ADMINISTRADOR:ADMIN,DIRECTOR_PRO:DIRECTOR_PROYECTO,GESTOR_PROYECTOS_TI:GESTOR_TIC,ANALISTA_PROYECTOS:CONSULTA",
            "Alias de roles externos normalizados contra roles funcionales internos"
        );

        crearParametroSiNoExiste(
            SystemParameterKeys.FURAG_RESPUESTAS_OBLIGATORIAS,
            "7",
            "Cantidad minima de respuestas FURAG para generar reportes"
        );

        crearParametroSiNoExiste(
            SystemParameterKeys.PETI_VIGENCIAS,
            "2020-2024,2024-2027,2027-2030",
            "Vigencias disponibles para el catalogo PETI"
        );

        crearParametroSiNoExiste(
            SystemParameterKeys.PETI_ESTRATEGIAS,
            "TECNOLOGIAS_INFORMACION:Tecnologias de la Informacion|TRANSFORMACION_DIGITAL:Transformacion Digital|CIUDADES_TERRITORIOS_INTELIGENTES:Ciudades y Territorios Inteligentes|GOBIERNO_DIGITAL:Gobierno Digital",
            "Estrategias PETI disponibles. Formato: CODIGO:Nombre separadas por |"
        );

        crearParametroSiNoExiste(
            SystemParameterKeys.RIESGO_PROBABILIDADES,
            "UNO,DOS,TRES,CUATRO,CINCO",
            "Catálogo de probabilidades de riesgo"
        );

        crearParametroSiNoExiste(
            SystemParameterKeys.RIESGO_IMPACTOS,
            "UNO,DOS,TRES,CUATRO,CINCO",
            "Catálogo de impactos de riesgo"
        );

        crearParametroSiNoExiste(
            SystemParameterKeys.RIESGO_NIVELES,
            "BAJO,MODERADO,ALTO,EXTREMO",
            "Catálogo de niveles de riesgo"
        );

        crearParametroSiNoExiste(
            SystemParameterKeys.STORAGE_BASE_PATH,
            "",
            "Ruta padre de almacenamiento de archivos. Si esta vacia se usa la ruta por defecto del servidor."
        );

        logger.info("Parametros del sistema cargados");
    }

    private void crearParametroSiNoExiste(String clave, String valor, String descripcion) {
        if (!parameterRepository.existsById(clave)) {
            SystemParameter param = new SystemParameter(clave, valor, descripcion);
            parameterRepository.save(param);
            logger.debug("Parametro creado: {}", clave);
        }
    }
}
