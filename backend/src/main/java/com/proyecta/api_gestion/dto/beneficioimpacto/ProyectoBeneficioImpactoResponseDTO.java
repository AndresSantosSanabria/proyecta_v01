package com.proyecta.api_gestion.dto.beneficioimpacto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(example = """
    {
      "id": 5101,
      "proyectoId": "PROY-CUN-2026-008",
      "proyectoNombre": "Modernización de Redes LAN",
      "estado": "DILIGENCIADO",
      "requeridoEn": "2026-07-01T08:00:00",
      "requeridoPor": "ana.gestion@proyecta.gov.co",
      "diligenciadoEn": "2026-07-15T10:30:00",
      "diligenciadoPor": "luis.coordinador@proyecta.gov.co",
      "revisadoEn": null,
      "revisadoPor": null,
      "beneficioPrincipal": "Reducción de tiempos de respuesta en los servicios digitales",
      "impactoSocial": "Mejora de la cobertura de internet en 8 sedes institucionales",
      "observaciones": null,
      "snapshotJson": "{"estado":"DILIGENCIADO","fecha":"2026-07-15"}",
      "creadoEn": "2026-07-01T08:00:00",
      "actualizadoEn": "2026-07-15T10:30:00",
      "requerido": true,
      "editable": true,
      "visibleParaGestor": true,
      "bloqueaCierre": false
    }
    """)
public record ProyectoBeneficioImpactoResponseDTO(
        Long id,
        String proyectoId,
        String proyectoNombre,
        String estado,
        LocalDateTime requeridoEn,
        String requeridoPor,
        LocalDateTime diligenciadoEn,
        String diligenciadoPor,
        LocalDateTime revisadoEn,
        String revisadoPor,
        String beneficioPrincipal,
        String impactoSocial,
        String observaciones,
        String snapshotJson,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn,
        boolean requerido,
        boolean editable,
        boolean visibleParaGestor,
        boolean bloqueaCierre
) {
}
