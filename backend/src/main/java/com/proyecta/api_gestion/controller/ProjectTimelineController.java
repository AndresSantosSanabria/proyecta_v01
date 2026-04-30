package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IProjectTimelineController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.timeline.TimelineResponseDTO;
import com.proyecta.api_gestion.service.interfaces.ProjectTimelineService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/proyectos/cronograma")
@CrossOrigin(origins = "*")
public class ProjectTimelineController implements IProjectTimelineController {

    private final ProjectTimelineService timelineService;

    public ProjectTimelineController(ProjectTimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @Override
    @GetMapping("/{id}/timeline")
    public ResponseEntity<ApiResponse<TimelineResponseDTO>> getProjectTimeline(@PathVariable String id) {
        TimelineResponseDTO response = timelineService.getProjectTimeline(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Cronograma obtenido exitosamente"));
    }

    @Override
    @GetMapping("/{id}/schedule-file")
    public ResponseEntity<Resource> downloadScheduleFile(@PathVariable String id) {
        Resource resource = timelineService.downloadScheduleFile(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @Override
    @PostMapping("/{id}/schedule-file")
    public ResponseEntity<ApiResponse<Void>> uploadScheduleFile(
            @PathVariable String id, @RequestParam("file") MultipartFile file) {
        timelineService.uploadScheduleFile(id, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Archivo PDF subido y asociado correctamente"));
    }
}
