package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.timeline.TimelineResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Cronograma de Proyectos", description = "Endpoints para la gestión de la planificación temporal y Gantt")
public interface IProjectTimelineController {

    @Operation(
        summary = "Obtener visualización del Gantt",
        description = "Retorna una lista estructurada de Fases e Hitos con fechas calculadas para renderizar el cronograma visual."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Timeline obtenido exitosamente",
            content = @Content(schema = @Schema(implementation = TimelineResponseDTO.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Proyecto no encontrado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Error en servicio de base de datos")
    })
    @StandardApiResponses
    @GetMapping("/{id}/timeline")
    ResponseEntity<ApiResponse<TimelineResponseDTO>> getProjectTimeline(
            @Parameter(description = "ID del proyecto") @PathVariable String id);

    @Operation(
        summary = "Descargar cronograma PDF",
        description = "Retorna el flujo de datos del archivo PDF almacenado para el proyecto."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Archivo descargado exitosamente",
            content = @Content(mediaType = "application/pdf")
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Archivo o proyecto no encontrado")
    })
    @StandardApiResponses
    @GetMapping("/{id}/schedule-file")
    ResponseEntity<Resource> downloadScheduleFile(
            @Parameter(description = "ID del proyecto") @PathVariable String id);

    @Operation(
        summary = "Subir cronograma PDF",
        description = "Carga y asocia un archivo PDF como el cronograma oficial del proyecto."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Archivo PDF subido y asociado correctamente"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Archivo no válido o excede tamaño"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El proyecto está cerrado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Fallo en el sistema de archivos")
    })
    @StandardApiResponses
    @PostMapping(value = "/{id}/schedule-file", consumes = "multipart/form-data")
    ResponseEntity<ApiResponse<Void>> uploadScheduleFile(
            @Parameter(description = "ID del proyecto") @PathVariable String id,
            @RequestPart("file") MultipartFile file);
}
