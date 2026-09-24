package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.advance.AdvanceReportStatusDTO;
import com.proyecta.api_gestion.dto.advance.AdvanceReportVersionDTO;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.advance.AdvanceReportUpload;
import com.proyecta.api_gestion.model.advance.AdvanceReportVersion;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.advance.AdvanceReportUploadRepository;
import com.proyecta.api_gestion.repository.advance.AdvanceReportVersionRepository;
import com.proyecta.api_gestion.service.advance.AdvanceReportNotificationService;
import com.proyecta.api_gestion.service.advance.AdvanceReportPeriodService;
import com.proyecta.api_gestion.service.advance.AdvanceReportRuleEvaluator;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import com.proyecta.api_gestion.service.impl.FileStorageServiceImpl;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.security.dynamic.ProyectoSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/advance-report")
@CrossOrigin(origins = "*")
public class AdvanceReportController {

    private static final Logger log = LoggerFactory.getLogger(AdvanceReportController.class);

    private final AdvanceReportRuleEvaluator ruleEvaluator;
    private final AdvanceReportNotificationService notificationService;
    private final AdvanceReportPeriodService periodService;
    private final ProyectoRepository proyectoRepository;
    private final AdvanceReportUploadRepository uploadRepository;
    private final AdvanceReportVersionRepository versionRepository;
    private final SystemParameterService systemParameterService;
    private final KeycloakIdentityExtractor identityExtractor;
    private final FileStorageServiceImpl fileStorageService;
    private final ProyectoSecurity proyectoSecurity;
    private final LocalUserAuthorizationService localUserAuthorizationService;

    public AdvanceReportController(
            AdvanceReportRuleEvaluator ruleEvaluator,
            AdvanceReportNotificationService notificationService,
            AdvanceReportPeriodService periodService,
            ProyectoRepository proyectoRepository,
            AdvanceReportUploadRepository uploadRepository,
            AdvanceReportVersionRepository versionRepository,
            SystemParameterService systemParameterService,
            KeycloakIdentityExtractor identityExtractor,
            FileStorageServiceImpl fileStorageService,
            ProyectoSecurity proyectoSecurity,
            LocalUserAuthorizationService localUserAuthorizationService) {
        this.ruleEvaluator = ruleEvaluator;
        this.notificationService = notificationService;
        this.periodService = periodService;
        this.proyectoRepository = proyectoRepository;
        this.uploadRepository = uploadRepository;
        this.versionRepository = versionRepository;
        this.systemParameterService = systemParameterService;
        this.identityExtractor = identityExtractor;
        this.fileStorageService = fileStorageService;
        this.proyectoSecurity = proyectoSecurity;
        this.localUserAuthorizationService = localUserAuthorizationService;
    }

    /**
     * Descarga el informe de avance. Params opcionales: periodo y version
     * (default: periodo vigente y version ACTUAL).
     */
    @GetMapping("/download/{projectId}")
    public ResponseEntity<Resource> downloadReport(
            @PathVariable String projectId,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) Integer version,
            Authentication authentication) {

        String pid = projectId.trim().toUpperCase();
        String periodoDef = (periodo == null || periodo.isBlank())
                ? periodService.currentPeriodo(LocalDate.now())
                : periodo.trim();

        AdvanceReportUpload upload = uploadRepository.findByProjectIdAndPeriodo(pid, periodoDef)
                .orElseThrow(() -> new ResourceNotFoundException("No existe informe de avance cargado para el periodo " + periodoDef + "."));

        AdvanceReportVersion versionEntity;
        if (version != null) {
            versionEntity = versionRepository.findByUploadIdAndNumeroVersion(upload.getId(), version)
                    .orElseThrow(() -> new ResourceNotFoundException("No existe la versión " + version + " del informe."));
        } else {
            versionEntity = versionRepository.findByUploadIdAndEstado(upload.getId(), AdvanceReportVersion.ESTADO_ACTUAL)
                    .or(() -> versionRepository.findByUploadIdOrderByNumeroVersionDesc(upload.getId()).stream().findFirst())
                    .orElseThrow(() -> new ResourceNotFoundException("El informe de avance no tiene versiones cargadas."));
        }

        Resource resource = fileStorageService.loadFileAsResource("informes-avance", versionEntity.getFilePath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + versionEntity.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
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
        String periodo = periodService.currentPeriodo(today);
        AdvanceReportUpload upload = notificationService.getUpload(proyecto.getId(), periodo);

        return ResponseEntity.ok(ApiResponse.success(
                buildStatusDTO(proyecto, upload, today, dueDate, periodo), "Estado del informe de avance"));
    }

    /**
     * Historial de versiones del informe de avance para un periodo.
     */
    @GetMapping("/versions/{projectId}")
    public ResponseEntity<ApiResponse<List<AdvanceReportVersionDTO>>> getVersions(
            @PathVariable String projectId,
            @RequestParam(required = false) String periodo) {

        String pid = projectId.trim().toUpperCase();
        String periodoDef = (periodo == null || periodo.isBlank())
                ? periodService.currentPeriodo(LocalDate.now())
                : periodo.trim();

        AdvanceReportUpload upload = uploadRepository.findByProjectIdAndPeriodo(pid, periodoDef)
                .orElseThrow(() -> new ResourceNotFoundException("No existe informe de avance para el periodo " + periodoDef + "."));

        List<AdvanceReportVersionDTO> versions = versionRepository
                .findByUploadIdOrderByNumeroVersionDesc(upload.getId()).stream()
                .map(v -> new AdvanceReportVersionDTO(
                        v.getNumeroVersion(), v.getFileName(), v.getFileSize(), v.getMimeType(),
                        v.getEstado(), v.getObservacion(), v.getSubidoPor(), v.getSubidoRol(),
                        v.getSubidoEn()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(versions, "Historial de versiones"));
    }

    /**
     * Retorna todos los proyectos con informe pendiente (para el modal de login).
     * Misma regla de elegibilidad y autorizacion que el scheduler.
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<AdvanceReportStatusDTO>>> getPendingProjects(
            Authentication authentication) {

        if (!periodService.isEnabled()) {
            return ResponseEntity.ok(ApiResponse.success(List.of(), "Proyectos con informe pendiente"));
        }

        LocalDate today = LocalDate.now();
        String periodo = periodService.currentPeriodo(today);
        LocalDate dueDate = ruleEvaluator.getDueDate();

        List<AdvanceReportStatusDTO> pending = proyectoRepository.findAll().stream()
                .filter(p -> periodService.esElegible(p, today))
                .filter(p -> proyectoSecurity.canAccessQuietly("PROYECTO:VER", p.getId(), authentication))
                .filter(p -> !notificationService.isUploaded(p.getId(), periodo))
                .map(p -> buildStatusDTO(p, null, today, dueDate, periodo))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(pending, "Proyectos con informe pendiente"));
    }

    /**
     * Retorna la configuracion actual del motor de reglas.
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSettings() {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("due_date", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_DUE_DATE, ""));
        settings.put("pre_due_window_days", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_PRE_DUE_WINDOW_DAYS, "15"));
        settings.put("pre_due_interval_days", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_PRE_DUE_INTERVAL_DAYS, "3"));
        settings.put("post_due_interval_days", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_POST_DUE_INTERVAL_DAYS, "7"));
        settings.put("specific_override_dates", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_SPECIFIC_OVERRIDE_DATES, ""));
        settings.put("allowed_extensions", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_ALLOWED_EXTENSIONS, "pdf,pptx"));
        settings.put("max_size_mb", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_MAX_SIZE_MB, "20"));
        settings.put("login_modal_delay_ms", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_LOGIN_MODAL_DELAY_MS, "1200"));
        settings.put("enabled", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_ENABLED, "true"));
        settings.put("min_project_age_months", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_MIN_PROJECT_AGE_MONTHS, "3"));
        settings.put("period_months", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_PERIOD_MONTHS, "3"));
        settings.put("due_day", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_DUE_DAY, "0"));
        settings.put("eligible_states", systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_ELIGIBLE_STATES, "ACTIVO,CON_RETRASOS,EN_REVISION"));
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
        updateIfPresent(SystemParameterKeys.ADVANCE_REPORT_ENABLED, settings.get("enabled"));
        updateIfPresent(SystemParameterKeys.ADVANCE_REPORT_MIN_PROJECT_AGE_MONTHS, settings.get("min_project_age_months"));
        updateIfPresent(SystemParameterKeys.ADVANCE_REPORT_PERIOD_MONTHS, settings.get("period_months"));
        updateIfPresent(SystemParameterKeys.ADVANCE_REPORT_DUE_DAY, settings.get("due_day"));
        updateIfPresent(SystemParameterKeys.ADVANCE_REPORT_ELIGIBLE_STATES, settings.get("eligible_states"));
        return ResponseEntity.ok(ApiResponse.success("OK", "Configuración actualizada. Los cambios se aplican en el próximo ciclo del scheduler."));
    }

    /**
     * Verifica (aprueba) un informe de avance. Rol gestor.
     */
    @PutMapping("/verify/{projectId}")
    @PreAuthorize("@proyectoSecurity.canReviewEvidence(#projectId, authentication)")
    public ResponseEntity<ApiResponse<String>> verifyReport(
            @PathVariable String projectId,
            Authentication authentication) {

        String pid = projectId.trim().toUpperCase();
        LocalDate today = LocalDate.now();
        String periodo = periodService.currentPeriodo(today);
        AdvanceReportUpload upload = uploadRepository.findByProjectIdAndPeriodo(pid, periodo)
                .orElseThrow(() -> new ResourceNotFoundException("No existe informe de avance para el periodo actual."));

        String actorVerify = identityExtractor.resolveUsername(authentication);
        upload.setEstado("VERIFICADO");
        upload.setVerifiedBy(actorVerify);
        upload.setVerifiedAt(LocalDateTime.now());
        uploadRepository.save(upload);

        Proyecto proyecto = proyectoRepository.findById(pid).orElse(null);
        notificationService.notificarInformeVerificado(proyecto, periodo, actorVerify);

        return ResponseEntity.ok(ApiResponse.success("Informe verificado exitosamente."));
    }

    /**
     * Devuelve un informe de avance al director con observaciones. Rol gestor.
     */
    @PutMapping("/return/{projectId}")
    @PreAuthorize("@proyectoSecurity.canReviewEvidence(#projectId, authentication)")
    public ResponseEntity<ApiResponse<String>> returnReport(
            @PathVariable String projectId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String observaciones = body.getOrDefault("observaciones", "");
        if (observaciones == null || observaciones.isBlank()) {
            throw new BadRequestException("Las observaciones son obligatorias para devolver el informe.");
        }

        String pid = projectId.trim().toUpperCase();
        LocalDate today = LocalDate.now();
        String periodo = periodService.currentPeriodo(today);
        AdvanceReportUpload upload = uploadRepository.findByProjectIdAndPeriodo(pid, periodo)
                .orElseThrow(() -> new ResourceNotFoundException("No existe informe de avance para el periodo actual."));

        String actorReturn = identityExtractor.resolveUsername(authentication);
        upload.setEstado("DEVUELTO");
        upload.setObservaciones(observaciones.trim());
        upload.setReturnedBy(actorReturn);
        upload.setReturnedAt(LocalDateTime.now());
        uploadRepository.save(upload);

        Proyecto proyecto = proyectoRepository.findById(pid).orElse(null);
        notificationService.notificarInformeDevuelto(proyecto, periodo, observaciones.trim(), actorReturn);

        return ResponseEntity.ok(ApiResponse.success("Informe devuelto al director."));
    }

    /**
     * Sube un informe de avance. Crea version nueva (ACTUAL/HISTORICA) y
     * permite re-cargar tras DEVUELTO/VERIFICADO. Rol director.
     */
    @PostMapping("/upload/{projectId}")
    @PreAuthorize("@proyectoSecurity.canUploadAdvanceReport(#projectId, authentication)")
    public ResponseEntity<ApiResponse<AdvanceReportStatusDTO>> uploadReport(
            @PathVariable String projectId,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        String pid = projectId.trim().toUpperCase();
        Proyecto proyecto = proyectoRepository.findById(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        LocalDate today = LocalDate.now();
        String periodo = periodService.currentPeriodo(today);
        String username = identityExtractor.resolveUsername(authentication);

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
        if ("pdf".equalsIgnoreCase(ext)) {
            validarPdfReal(file);
        }

        AdvanceReportUpload upload = uploadRepository.findByProjectIdAndPeriodo(pid, periodo).orElse(null);
        boolean esNuevaCabecera = upload == null;

        int siguienteVersion = esNuevaCabecera ? 1 : versionRepository.findMaxNumeroVersion(upload.getId()) + 1;
        String storedName = String.format("%s_%s_v%d_%s_%s", pid, periodo, siguienteVersion, sanitize(username), originalFilename);
        String filePath = fileStorageService.storeFile(file, "informes-avance", storedName);

        if (esNuevaCabecera) {
            upload = new AdvanceReportUpload(pid, periodo, originalFilename, filePath, file.getSize(), username);
            upload.setSubidoRol(resolveRol(authentication));
            upload = uploadRepository.save(upload);
        }

        versionRepository.findByUploadIdAndEstado(upload.getId(), AdvanceReportVersion.ESTADO_ACTUAL)
                .ifPresent(actual -> {
                    actual.setEstado(AdvanceReportVersion.ESTADO_HISTORICA);
                    versionRepository.save(actual);
                });

        AdvanceReportVersion version = new AdvanceReportVersion(
                upload.getId(), pid, periodo, siguienteVersion,
                originalFilename, filePath, file.getSize(), file.getContentType(),
                username, resolveRol(authentication));
        versionRepository.save(version);

        upload.setFileName(originalFilename);
        upload.setFilePath(filePath);
        upload.setFileSize(file.getSize());
        upload.setUploadedBy(username);
        upload.setUploadedAt(LocalDateTime.now());
        upload.setSubidoRol(resolveRol(authentication));
        upload.setEstado("PENDIENTE");
        upload.setObservaciones(null);
        upload.setVerifiedBy(null);
        upload.setVerifiedAt(null);
        upload.setReturnedBy(null);
        upload.setReturnedAt(null);
        upload = uploadRepository.save(upload);

        log.info("Informe de avance cargado: proyecto {} periodo {} version {} archivo {} por {}",
                pid, periodo, siguienteVersion, originalFilename, username);

        notificationService.notificarInformeCargado(proyecto, periodo, originalFilename, username);

        LocalDate dueDate = ruleEvaluator.getDueDate();
        return ResponseEntity.ok(ApiResponse.success(
                buildStatusDTO(proyecto, upload, today, dueDate, periodo),
                "Informe de avance cargado exitosamente."));
    }

    private AdvanceReportStatusDTO buildStatusDTO(Proyecto proyecto, AdvanceReportUpload upload,
                                                  LocalDate today, LocalDate dueDate, String periodo) {
        boolean uploaded = upload != null;
        return new AdvanceReportStatusDTO(
                proyecto.getId(),
                proyecto.getNombre(),
                !uploaded,
                uploaded,
                dueDate,
                dueDate != null ? ruleEvaluator.getDaysUntilDue(today) : Long.MAX_VALUE,
                ruleEvaluator.isOverdue(today),
                periodo,
                uploaded ? upload.getFileName() : null,
                uploaded && upload.getUploadedAt() != null ? upload.getUploadedAt().toString() : null,
                uploaded ? upload.getUploadedBy() : null,
                uploaded ? upload.getEstado() : null,
                uploaded ? upload.getObservaciones() : null,
                uploaded ? upload.getVerifiedBy() : null,
                uploaded ? upload.getReturnedBy() : null
        );
    }

    private void validarPdfReal(MultipartFile file) {
        try (var is = file.getInputStream()) {
            byte[] encabezado = is.readNBytes(5);
            String firma = new String(encabezado, StandardCharsets.US_ASCII);
            if (!firma.startsWith("%PDF-")) {
                throw new BadRequestException("El archivo cargado no es un PDF válido.");
            }
        } catch (IOException ex) {
            throw new BadRequestException("No fue posible validar el archivo PDF cargado.");
        }
    }

    private String resolveRol(Authentication authentication) {
        try {
            return localUserAuthorizationService.requireLocalUser(authentication).getRolCodigo();
        } catch (Exception e) {
            return null;
        }
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) return "usuario";
        return value.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void updateIfPresent(String key, String value) {
        if (value != null) {
            systemParameterService.set(key, value);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).trim();
    }
}
