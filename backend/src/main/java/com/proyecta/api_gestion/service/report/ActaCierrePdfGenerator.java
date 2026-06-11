package com.proyecta.api_gestion.service.report;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ActaCierrePdfGenerator {

    public record EntregableActaItem(
            String nombre,
            String fechaEntrega,
            String evidencia,
            String estado
    ) {
    }

    public record ActaCierrePdfData(
            String codigoProyecto,
            String nombreProyecto,
            String patrocinadorNombre,
            String patrocinadorCargo,
            String patrocinadorEntidad,
            String directorNombre,
            String directorCargo,
            String directorEntidad,
            String fechaInicio,
            String fechaCierre,
            String duracionTotalMeses,
            String objetivoGeneral,
            List<String> objetivosEspecificos,
            String resumenEjecutivo,
            String leccionesPositivas,
            String leccionesMejorar,
            String recomendaciones,
            String transferenciaActividad,
            String transferenciaFecha,
            String transferenciaUbicacionEvidencia,
            String avanceFinal,
            String progresoProgramadoFinal,
            String progresoEjecutadoFinal,
            String diferenciaFinal,
            String eficaciaFinal,
            String estadoFinal,
            List<EntregableActaItem> entregables
    ) {
    }

    public byte[] build(ActaCierrePdfData data) {
        List<String> lines = new ArrayList<>();

        lines.add("HDR|ACTA DE CIERRE DEL PROYECTO");
        lines.add("TXT|Documento institucional generado automaticamente al cerrar el proyecto.");
        lines.add("SEC|1. INFORMACION GENERAL DE PROYECTO");
        lines.add("KV|Codigo/ID del proyecto|" + safe(data.codigoProyecto()));
        lines.add("KV|Nombre del proyecto|" + safe(data.nombreProyecto()));
        lines.add("KV|Patrocinador|" + safe(data.patrocinadorNombre()));
        lines.add("KV|Cargo del patrocinador|" + safe(data.patrocinadorCargo()));
        lines.add("KV|Entidad del patrocinador|" + safe(data.patrocinadorEntidad()));
        lines.add("KV|Director del proyecto|" + safe(data.directorNombre()));
        lines.add("KV|Cargo del director|" + safe(data.directorCargo()));
        lines.add("KV|Entidad del director|" + safe(data.directorEntidad()));
        lines.add("KV|Fecha de inicio|" + safe(data.fechaInicio()));
        lines.add("KV|Fecha de cierre|" + safe(data.fechaCierre()));
        lines.add("KV|Duracion total|" + safe(data.duracionTotalMeses()) + " meses");

        lines.add("SEC|2. OBJETIVO GENERAL");
        lines.add("TXT|" + safe(data.objetivoGeneral()));

        lines.add("SEC|3. OBJETIVOS ESPECIFICOS");
        if (data.objetivosEspecificos() == null || data.objetivosEspecificos().isEmpty()) {
            lines.add("TXT|No se registraron objetivos especificos.");
        } else {
            data.objetivosEspecificos().stream()
                    .filter(Objects::nonNull)
                    .forEach(obj -> lines.add("BUL|" + safe(obj)));
        }

        lines.add("SEC|4. RESUMEN EJECUTIVO");
        lines.add("TXT|" + safe(data.resumenEjecutivo()));

        lines.add("SEC|5. ENTREGABLES DEL PROYECTO");
        lines.add("TABLE|3,1.1,0.9,0.9|Nombre del entregable|Fecha de entrega|Evidencia|Estado");
        if (data.entregables() == null || data.entregables().isEmpty()) {
            lines.add("ROW|Sin entregables registrados|N/A|N/A|N/A");
        } else {
            data.entregables().forEach(item -> lines.add(
                    "ROW|" + safe(item.nombre()) + "|" + safe(item.fechaEntrega()) + "|" + safe(item.evidencia()) + "|" + safe(item.estado())
            ));
        }

        lines.add("SEC|6. LECCIONES APRENDIDAS");
        lines.add("SUB|Aspectos positivos");
        lines.add("TXT|" + safe(data.leccionesPositivas()));
        lines.add("SUB|Aspectos a mejorar");
        lines.add("TXT|" + safe(data.leccionesMejorar()));
        lines.add("SUB|Recomendaciones para futuros proyectos");
        lines.add("TXT|" + safe(data.recomendaciones()));

        lines.add("SEC|7. TRANSFERENCIA DE CONOCIMIENTO");
        lines.add("KV|Actividad ejecutada|" + safe(data.transferenciaActividad()));
        lines.add("KV|Fecha|" + safe(data.transferenciaFecha()));
        lines.add("KV|Ubicacion de evidencia|" + safe(data.transferenciaUbicacionEvidencia()));

        lines.add("SEC|8. APROBACION DEL PATROCINADOR");
        lines.add("TXT|El patrocinador del proyecto y el director del proyecto dejan constancia del cierre formal del proyecto y de la aceptacion de la informacion consolidada en la presente acta.");
        lines.add("KV|Nombre del director del proyecto|" + safe(data.directorNombre()));
        lines.add("KV|Nombre del patrocinador del proyecto|" + safe(data.patrocinadorNombre()));
        lines.add("KV|Firma del director del proyecto|________________________________");
        lines.add("KV|Firma del patrocinador del proyecto|________________________________");
        lines.add("KV|Fecha|" + safe(data.fechaCierre()));

        lines.add("SEC|9. CONSOLIDADO DE CIERRE");
        lines.add("CARD|Avance final|" + safe(data.avanceFinal()) + "|blue");
        lines.add("CARD|Progreso programado|" + safe(data.progresoProgramadoFinal()) + "|info");
        lines.add("CARD|Progreso ejecutado|" + safe(data.progresoEjecutadoFinal()) + "|success");
        lines.add("CARD|Diferencia|" + safe(data.diferenciaFinal()) + "|warning");
        lines.add("CARD|Eficacia|" + safe(data.eficaciaFinal()) + "|blue");
        lines.add("CARD|Estado final|" + safe(data.estadoFinal()) + "|green");

        return SimplePdfReportBuilder.build("Acta de Cierre del Proyecto " + safe(data.codigoProyecto()), lines);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "No registrado" : value.trim();
    }
}
