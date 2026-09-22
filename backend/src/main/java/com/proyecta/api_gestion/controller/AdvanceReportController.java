package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.advance.AdvanceReportStatusDTO;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.advance.AdvanceReportUpload;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.advance.AdvanceReportUploadRepository;
import com.proyecta.api_gestion.service.advance.AdvanceReportNotificationService;
import com.proyecta.api_gestion.service.advance.AdvanceReportRuleEvaluator;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import com.proyecta.api_gestion.service.impl.FileStorageServiceImpl;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/v1/advance-report")
@CrossOrigin(origins = "*")
public class AdvanceReportController {

    private static final Logger log = LoggerFactory.getLogger(AdvanceReportController.class);
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AdvanceReportRuleEvaluator ruleEvaluator;
    private final AdvanceReportNotificationService notificationService;
    private final ProyectoRepository proyectoRepository;
    private final AdvanceReportUploadRepository uploadRepository;
    private final SystemParameterService systemParameterService;
    private final KeycloakIdentityExtractor identityExtractor;
    private final FileStorageServiceImpl fileStorageService;

    public AdvanceReportController(
            AdvanceReportRuleEvaluator ruleEvaluator,
            AdvanceReportNotificationService notificationService,
            ProyectoRepository proyectoRepository,
            AdvanceReportUploadRepository uploadRepository,
            SystemParameterService systemParameterService,
            KeycloakIdentityExtractor identityExtractor,
            FileStorageServiceImpl fileStorageService) {
        this.ruleEvaluator = ruleEvaluator;
        this.notificationService = notificationService;
        this.proyectoRepository = proyectoRepository;
        this.uploadRepository = uploadRepository;
        this.systemParameterService = systemParameterService;
        this.identityExtractor = identityExtractor;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Retorna el estado del informe de avance para un proyecto.
     */
    /**
     * Descarga el informe de avance del periodo actual.
     */
    @GetMapping("/download/{projectId}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadReport(
            @PathVariable String projectId,
            Authentication authentication) {

        LocalDate today = LocalDate.now();
        String periodo = resolveCurrentPeriodo(today);
        AdvanceReportUpload upload = uploadRepository.findByProjectIdAndPeriodo(projectId.trim().toUpperCase(), periodo)
                .orElseThrow(() -> new ResourceNotFoundException("No existe informe de avance cargado para este periodo."));

        org.springframework.core.io.Resource resource = fileStorageService.loadFileAsResource("informes-avance", upload.getFilePath());

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + upload.getFileName() + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/status/{projectId}")
    public ResponseEntity<ApiResponse<AdvanceReportStatusDTO>> getStatus(
            @PathVariable String projectId,
            Authentication authentication) {

        Proyecto proyecto = proyectoRepository.findById(projectId.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        LocalDate today = LocalDate.now();
        LocalDate dueDate = ruleEvaluator.getDueDate();
        String periodo = resolveCurrentPeriodo(today);
        AdvanceReportUpload upload = notificationService.getUpload(projectId, periodo);

        AdvanceReportStatusDTO dto = new AdvanceReportStatusDTO(
                proyecto.getId(),
                proyecto.getNombre(),
                upload == null,
                upload != null,
                dueDate,
                dueDate != null ? ruleEvaluator.getDaysUntilDue(today) : Long.MAX_VALUE,
                ruleEvaluator.isOverdue(today),
                periodo,
                upload != null ? upload.getFileName() : null,
                upload != null ? upload.getUploadedAt().toString() : null,
                upload != null ? upload.getUploadedBy() : null,
                upload != null ? upload.getEstado() : null,
                upload != null ? upload.getObservaciones() : null
        );

        return ResponseEntity.ok(ApiResponse.success(dto, "Estado del informe de avance"));
    }

    /**
     * Retorna todos los proyectos con informe pendiente (para el modal de login).
     * Accesible para cualquier usuario autenticado; filtra por proyectos asignados si no es transversal.
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<AdvanceReportStatusDTO>>> getPendingProjects(
            Authentication authentication) {

        LocalDate today = LocalDate.now();
        LocalDate dueDate = ruleEvaluator.getDueDate();
        String periodo = resolveCurrentPeriodo(today);
        
        String username = identityExtractor.resolveUsername(authentication);
        boolean isTransversal = authentication.getAuthorities().stream()
                .anyMatch(a -> {
                    String auth = a.getAuthority().toLowerCase();
                    return auth.contains("admin") ||
                           auth.contains("pm_office") ||
                           auth.contains("gerente_portafolio") ||
                           auth.contains("lider_funcional") ||
                           auth.contains("auditor");
                });

        List<AdvanceReportStatusDTO> pending = proyectoRepository.findAll().stream()
                .filter(p -> p.getEstado() != null
                        && !com.proyecta.api_gestion.model.enums.EstadoProyecto.CERRADO.equals(p.getEstado())
                        && !com.proyecta.api_gestion.model.enums.EstadoProyecto.CERRADO_FORZOSO.equals(p.getEstado())
                        && !com.proyecta.api_gestion.model.enums.EstadoProyecto.FINALIZADO.equals(p.getEstado()))
                .filter(p -> {
                    if (isTransversal) return true;
                    // Filter if user is director (via direct repository query to avoid LazyInitializationException)
                    return proyectoRepository.existsDirectorByProyectoIdAndUsername(p.getId(), username);
                })
                .filter(p -> !notificationService.isUploaded(p.getId(), periodo))
                .map(p -> new AdvanceReportStatusDTO(
                        p.getId(),
                        p.getNombre(),
                        true,
                        false,
                        dueDate,
                        dueDate != null ? ruleEvaluator.getDaysUntilDue(today) : Long.MAX_VALUE,
                        ruleEvaluator.isOverdue(today),
                        periodo,
                        null,
                        null,
                        null,
                        null,
                        null
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(pending, "Proyectos con informe pendiente"));
    }

    /**
     * Retorna la configuracion actual del motor de reglas.
     * Lectura accesible para cualquier usuario autenticado.
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSettings() {
        Map<String, String> settings = Map.of(
                "due_date", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_DUE_DATE, ""),
                "pre_due_window_days", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_PRE_DUE_WINDOW_DAYS, "15"),
                "pre_due_interval_days", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_PRE_DUE_INTERVAL_DAYS, "3"),
                "post_due_interval_days", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_POST_DUE_INTERVAL_DAYS, "7"),
                "specific_override_dates", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_SPECIFIC_OVERRIDE_DATES, ""),
                "allowed_extensions", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_ALLOWED_EXTENSIONS, "pdf,pptx"),
                "max_size_mb", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_MAX_SIZE_MB, "20"),
                "login_modal_delay_ms", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_LOGIN_MODAL_DELAY_MS, "1200")
        );
        return ResponseEntity.ok(ApiResponse.success(settings, "Configuración del motor de reglas"));
    }

    /**
     * Actualiza la configuración del motor de reglas (admin).
     */
    @PutMapping("/settings")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<String>> updateSettings(@RequestBody Map<String, String> settings) {
        updateIfPresent(SystemParameterKeys.ADVANCE_REPORT_DUE_DATE, settings.get("due_date"));
        updateIfPresent(SystemParameterKeys.ADVANCE_REPORT_PRE_DUE_WINDOW_DAYS, settings.get("pre_due_window_days"));
        updateIfPresent(SystemParameterKeys.ADVANCE_REPORT_PRE_DUE_INTERVAL_DAYS, settings.get("pre_due_interval_days"));
        updateIfPresent(SystemParameterKeys.ADVANCE_REPORT_POST_DUE_INTERVAL_DAYS, settings.get("post_due_interval_days"));
        updateIfPresent(SystemParameterKeys.ADVANCE_REPORT_SPECIFIC_OVERRIDE_DATES, settings.get("specific_override_dates"));
        updateIfPresent(SystemParameterKeys.ADVANCE_REPORT_ALLOWED_EXTENSIONS, settings.get("allowed_extensions"));
        updateIfPresent(SystemParameterKeys.ADVANCE_REPORT_MAX_SIZE_MB, settings.get("max_size_mb"));
        updateIfPresent(SystemParameterKeys.ADVANCE_REPORT_LOGIN_MODAL_DELAY_MS, settings.get("login_modal_delay_ms"));
        return ResponseEntity.ok(ApiResponse.success("OK", "Configuración actualizada. Los cambios se aplican en el próximo ciclo del scheduler."));
    }

    /**
     * Verifica (aprueba) un informe de avance.
     */
    @PutMapping("/verify/{projectId}")
    public ResponseEntity<ApiResponse<String>> verifyReport(
            @PathVariable String projectId,
            Authentication authentication) {

        LocalDate today = LocalDate.now();
        String periodo = resolveCurrentPeriodo(today);
        AdvanceReportUpload upload = uploadRepository.findByProjectIdAndPeriodo(projectId.trim().toUpperCase(), periodo)
                .orElseThrow(() -> new ResourceNotFoundException("No existe informe de avance para el periodo actual."));

        upload.setEstado("VERIFICADO");
        uploadRepository.save(upload);

        return ResponseEntity.ok(ApiResponse.success("Informe verificado exitosamente."));
    }

    /**
     * Devuelve un informe de avance al director con observaciones.
     */
    @PutMapping("/return/{projectId}")
    public ResponseEntity<ApiResponse<String>> returnReport(
            @PathVariable String projectId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String observaciones = body.getOrDefault("observaciones", "");
        LocalDate today = LocalDate.now();
        String periodo = resolveCurrentPeriodo(today);
        AdvanceReportUpload upload = uploadRepository.findByProjectIdAndPeriodo(projectId.trim().toUpperCase(), periodo)
                .orElseThrow(() -> new ResourceNotFoundException("No existe informe de avance para el periodo actual."));

        upload.setEstado("DEVUELTO");
        upload.setObservaciones(observaciones);
        uploadRepository.save(upload);

        return ResponseEntity.ok(ApiResponse.success("Informe devuelto al director."));
    }

    /**
     * Sube un informe de avance para un proyecto y periodo específico.
     */
    @PostMapping("/upload/{projectId}")
    public ResponseEntity<ApiResponse<AdvanceReportStatusDTO>> uploadReport(
            @PathVariable String projectId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        Proyecto proyecto = proyectoRepository.findById(projectId.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        LocalDate today = LocalDate.now();
        String periodo = resolveCurrentPeriodo(today);
        String username = identityExtractor.resolveUsername(authentication);

        // Validate file
        Set<String> allowedExtensions = new HashSet<>(Arrays.asList(
                systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_ALLOWED_EXTENSIONS, "pdf,pptx")
                        .split(",")));
        long maxSizeBytes = systemParameterService.getLong(SystemParameterKeys.ADVANCE_REPORT_MAX_SIZE_MB, 20) * 1024 * 1024;

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "sin_nombre";
        String ext = extractExtension(originalFilename);
        if (!allowedExtensions.contains(ext.toLowerCase())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Extensión no permitida: " + ext
                    + ". Permitidas: " + allowedExtensions));
        }
        if (file.getSize() > maxSizeBytes) {
            return ResponseEntity.badRequest().body(ApiResponse.error("El archivo supera el tamaño máximo permitido de "
                    + (maxSizeBytes / 1024 / 1024) + " MB."));
        }

        // Store file
        String storedName = String.format("%s_%s_%s_%s", projectId, periodo, username, originalFilename);
        String filePath = fileStorageService.storeFile(file, "informes-avance", storedName);

        // Save record
        AdvanceReportUpload upload = new AdvanceReportUpload(
                proyecto.getId(), periodo, originalFilename, filePath, file.getSize(), username);
        uploadRepository.save(upload);

        log.info("Informe de avance cargado: proyecto {} periodo {} archivo {} por {}",
                projectId, periodo, originalFilename, username);

        LocalDate dueDate = ruleEvaluator.getDueDate();
        AdvanceReportStatusDTO dto = new AdvanceReportStatusDTO(
                proyecto.getId(),
                proyecto.getNombre(),
                false,
                true,
                dueDate,
                dueDate != null ? ruleEvaluator.getDaysUntilDue(today) : Long.MAX_VALUE,
                ruleEvaluator.isOverdue(today),
                periodo,
                originalFilename,
                upload.getUploadedAt().toString(),
                username,
                "PENDIENTE",
                null
        );

        return ResponseEntity.ok(ApiResponse.success(dto, "Informe de avance cargado exitosamente."));
    }

    private void updateIfPresent(String key, String value) {
        if (value != null) {
            systemParameterService.set(key, value);
        }
    }

    private String resolveCurrentPeriodo(LocalDate date) {
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        return String.format("%d-Q%d", date.getYear(), quarter);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).trim();
    }
}
