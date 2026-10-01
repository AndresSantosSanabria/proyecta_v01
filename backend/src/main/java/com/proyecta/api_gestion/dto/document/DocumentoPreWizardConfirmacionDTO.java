package com.proyecta.api_gestion.dto.document;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"evento":"DOCUMENTOS_VERIFICADOS","mensaje":"Documentos pre-wizard verificados correctamente","documentosStatuses":"VIABILIZACION:APROBADO, ACTA_CONSTITUCION:APROBADO"}
    """)
public record DocumentoPreWizardConfirmacionDTO(
        String evento,
        String mensaje,
        String documentStatuses
) {
}
