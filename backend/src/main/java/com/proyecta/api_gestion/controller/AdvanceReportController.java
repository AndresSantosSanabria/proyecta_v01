package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.advance.AdvanceReportStatusDTO;
import com.proyecta.api_gestion.dto.advance.AdvanceReportVersionDTO;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.domain.exception.BadRequestException;
import com.proyecta.api_gestion.domain.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.advance.AdvanceReportUpload;
import com.proyecta.api_gestion.domain.model.advance.AdvanceReportVersion;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.advance.AdvanceReportUploadRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.advance.AdvanceReportVersionRepositoryPort;
import com.proyecta.api_gestion.service.advance.AdvanceReportNotificationService;
import com.proyecta.api_gestion.service.advance.AdvanceReportPeriodService;
import com.proyecta.api_gestion.service.advance.AdvanceReportRuleEvaluator;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import com.proyecta.api_gestion.service.impl.FileStorageServiceImpl;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.security.dynamic.ProyectoSecurity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/v1/advance-report")
@Tag(name = "Reporte de Avance", description = "Endpoints para la generación, revisión y devolución del reporte PDF de avance")
public class AdvanceReportController {

    private static final Logger log = LoggerFactory.getLogger(AdvanceReportController.class);

    private final AdvanceReportRuleEvaluator ruleEvaluator;
    private final AdvanceReportNotificationService notificationService;
    private final AdvanceReportPeriodService periodService;
    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final AdvanceReportUploadRepositoryPort uploadRepositoryPort;
    private final AdvanceReportVersionRepositoryPort versionRepositoryPort;
    private final SystemParameterService systemParameterService;
    private final KeycloakIdentityExtractor identityExtractor;
    private final FileStorageServiceImpl fileStorageService;
    private final ProyectoSecurity proyectoSecurity;
    private final LocalUserAuthorizationService localUserAuthorizationService;

    @Autowired
    public AdvanceReportController(
            AdvanceReportRuleEvaluator ruleEvaluator,
            AdvanceReportNotificationService notificationService,
            AdvanceReportPeriodService periodService,
            ProyectoRepositoryPort proyectoRepositoryPort,
            AdvanceReportUploadRepositoryPort uploadRepositoryPort,
            AdvanceReportVersionRepositoryPort versionRepositoryPort,
            SystemParameterService systemParameterService,
            KeycloakIdentityExtractor identityExtractor,
            FileStorageServiceImpl fileStorageService,
            ProyectoSecurity proyectoSecurity,
            LocalUserAuthorizationService localUserAuthorizationService) {
        this.ruleEvaluator = ruleEvaluator;
        this.notificationService = notificationService;
        this.periodService = periodService;
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.uploadRepositoryPort = uploadRepositoryPort;
        this.versionRepositoryPort = versionRepositoryPort;
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
    @Operation(summary = "Descargar el PDF del informe de avance", description = "Si no se indican periodo ni versión se usa el periodo vigente y la versión ACTUAL.")
    @GetMapping("/download/{projectId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #projectId, authentication)")
    public ResponseEntity<Resource> downloadReport(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId,
            @Parameter(description = "Periodo del informe (AAAA-MM), por defecto el vigente") @RequestParam(required = false) String periodo,
            @Parameter(description = "Número de versión del informe, por defecto la versión ACTUAL") @RequestParam(required = false) Integer version,
            Authentication authentication) {

        String pid = projectId.trim().toUpperCase();
        String periodoDef = (periodo == null || periodo.isBlank())
                ? periodService.currentPeriodo(LocalDate.now(ZoneId.systemDefault()))
                : periodo.trim();

        AdvanceReportUpload upload = uploadRepositoryPort.findByProjectIdAndPeriodo(pid, periodoDef)
                .orElseThrow(() -> new ResourceNotFoundException("No existe informe de avance cargado para el periodo " + periodoDef + "."));

        AdvanceReportVersion versionEntity;
        if (version != null) {
            versionEntity = versionRepositoryPort.findByUploadIdAndNumeroVersion(upload.getId(), version)
                    .orElseThrow(() -> new ResourceNotFoundException("No existe la versión " + version + " del informe."));
        } else {
            versionEntity = versionRepositoryPort.findByUploadIdAndEstado(upload.getId(), AdvanceReportVersion.ESTADO_ACTUAL)
                    .or(() -> versionRepositoryPort.findByUploadIdOrderByNumeroVersionDesc(upload.getId()).stream().findFirst())
                    .orElseThrow(() -> new ResourceNotFoundException("El informe de avance no tiene versiones cargadas."));
        }

        Resource resource = fileStorageService.loadFileAsResource("informes-avance", versionEntity.getFilePath());

        // CWE-113: nunca ecoar el nombre original sin sanear en la cabecera.
        String safeFileName = com.proyecta.api_gestion.infrastructure.HttpHeaderSanitizer.safeFileName(upload.getFileName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        com.proyecta.api_gestion.infrastructure.HttpHeaderSanitizer.contentDisposition("attachment", safeFileName))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @Operation(summary = "Obtener el estado del informe de avance de un proyecto")
    @GetMapping("/status/{projectId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #projectId, authentication)")
    public ResponseEntity<ApiResponse<AdvanceReportStatusDTO>> getStatus(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId,
            Authentication authentication) {

        Proyecto proyecto = proyectoRepositoryPort.findById(projectId.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate dueDate = ruleEvaluator.getDueDate();
        String periodo = periodService.currentPeriodo(today);
        AdvanceReportUpload upload = notificationService.getUpload(proyecto.getId(), periodo);

        return ResponseEntity.ok(ApiResponse.success(
                buildStatusDTO(proyecto, upload, today, dueDate, periodo), "Estado del informe de avance"));
    }

    /**
     * Historial de versiones del informe de avance para un periodo.
     */
    @Operation(summary = "Listar el historial de versiones del informe de avance", description = "Si no se indica periodo se usa el periodo vigente.")
    @GetMapping("/versions/{projectId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #projectId, authentication)")
    public ResponseEntity<ApiResponse<List<AdvanceReportVersionDTO>>> getVersions(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId,
            @Parameter(description = "Periodo del informe (AAAA-MM), por defecto el vigente") @RequestParam(required = false) String periodo) {

        String pid = projectId.trim().toUpperCase();
        String periodoDef = (periodo == null || periodo.isBlank())
                ? periodService.currentPeriodo(LocalDate.now(ZoneId.systemDefault()))
                : periodo.trim();

        AdvanceReportUpload upload = uploadRepositoryPort.findByProjectIdAndPeriodo(pid, periodoDef)
                .orElseThrow(() -> new ResourceNotFoundException("No existe informe de avance para el periodo " + periodoDef + "."));

        List<AdvanceReportVersionDTO> versions = versionRepositoryPort
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
    @Operation(summary = "Listar proyectos con informe de avance pendiente", description = "Aplica la misma regla de elegibilidad y autorización que el programador de avisos; se usa en el modal de login.")
    @GetMapping("/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<AdvanceReportStatusDTO>>> getPendingProjects(
            Authentication authentication) {

        if (!periodService.isEnabled()) {
            return ResponseEntity.ok(ApiResponse.success(List.of(), "Proyectos con informe pendiente"));
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String periodo = periodService.currentPeriodo(today);
        LocalDate dueDate = ruleEvaluator.getDueDate();

        List<AdvanceReportStatusDTO> pending = proyectoRepositoryPort.findAll().stream()
                .filter(p -> periodService.esElegible(p, today))
                .filter(p -> proyectoSecurity.canAccessQuietly("PROYECTO:VER", p.getId(), authentication))
                .filter(p -> !notificationService.isUploaded(p.getId(), periodo))
                .map(p -> buildStatusDTO(p, null, today, dueDate, periodo))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(pending, "Proyectos con informe pendiente"));
    }

    /**
     * Retorna la configuracion actual del motor de reglas.
     * Solo usuarios autenticados (lo consume el login-check de todos los roles);
     * no contiene secretos. La escritura exige SISTEMA:CONFIGURAR.
     */
    @Operation(summary = "Obtener la configuración del motor de reglas del informe de avance", description = "No contiene secretos; solo requiere un usuario autenticado.")
    @GetMapping("/settings")
    @PreAuthorize("isAuthenticated()")
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
    @Operation(summary = "Actualizar la configuración del motor de reglas", description = "Requiere el permiso SISTEMA:CONFIGURAR. Los cambios se aplican en el próximo ciclo del programador de avisos.")
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
    @Operation(summary = "Verificar (aprobar) el informe de avance del periodo actual", description = "Rol con permiso de revisión; notifica al director que el informe fue verificado.")
    @PutMapping("/verify/{projectId}")
    @PreAuthorize("@proyectoSecurity.canReviewAdvanceReport(#projectId, authentication)")
    public ResponseEntity<ApiResponse<String>> verifyReport(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId,
            Authentication authentication) {

        String pid = projectId.trim().toUpperCase();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String periodo = periodService.currentPeriodo(today);
        AdvanceReportUpload upload = uploadRepositoryPort.findByProjectIdAndPeriodo(pid, periodo)
                .orElseThrow(() -> new ResourceNotFoundException("No existe informe de avance para el periodo actual."));

        String actorVerify = identityExtractor.resolveUsername(authentication);
        upload.setEstado("VERIFICADO");
        upload.setVerifiedBy(actorVerify);
        upload.setVerifiedAt(LocalDateTime.now(ZoneId.systemDefault()));
        uploadRepositoryPort.save(upload);

        Proyecto proyecto = proyectoRepositoryPort.findById(pid).orElse(null);
        notificationService.notificarInformeVerificado(proyecto, periodo, actorVerify);

        return ResponseEntity.ok(ApiResponse.success("Informe verificado exitosamente."));
    }

    /**
     * Devuelve un informe de avance al director con observaciones. Rol gestor.
     */
    @Operation(summary = "Devolver el informe de avance al director con observaciones", description = "Las observaciones son obligatorias y se notifican al director.")
    @PutMapping("/return/{projectId}")
    @PreAuthorize("@proyectoSecurity.canReviewAdvanceReport(#projectId, authentication)")
    public ResponseEntity<ApiResponse<String>> returnReport(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String observaciones = body.getOrDefault("observaciones", "");
        if (observaciones == null || observaciones.isBlank()) {
            throw new BadRequestException("Las observaciones son obligatorias para devolver el informe.");
        }

        String pid = projectId.trim().toUpperCase();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String periodo = periodService.currentPeriodo(today);
        AdvanceReportUpload upload = uploadRepositoryPort.findByProjectIdAndPeriodo(pid, periodo)
                .orElseThrow(() -> new ResourceNotFoundException("No existe informe de avance para el periodo actual."));

        String actorReturn = identityExtractor.resolveUsername(authentication);
        upload.setEstado("DEVUELTO");
        upload.setObservaciones(observaciones.trim());
        upload.setReturnedBy(actorReturn);
        upload.setReturnedAt(LocalDateTime.now(ZoneId.systemDefault()));
        uploadRepositoryPort.save(upload);

        Proyecto proyecto = proyectoRepositoryPort.findById(pid).orElse(null);
        notificationService.notificarInformeDevuelto(proyecto, periodo, observaciones.trim(), actorReturn);

        return ResponseEntity.ok(ApiResponse.success("Informe devuelto al director."));
    }

    /**
     * Sube un informe de avance. Crea version nueva (ACTUAL/HISTORICA) y
     * permite re-cargar tras DEVUELTO/VERIFICADO. Rol director.
     */
    @Operation(summary = "Cargar el informe de avance del proyecto", description = "Crea una versión nueva y permite recargar tras DEVUELTO o VERIFICADO. Valida extensión, tamaño y contenido real del archivo. Rol director.")
    @PostMapping("/upload/{projectId}")
    @PreAuthorize("@proyectoSecurity.canUploadAdvanceReport(#projectId, authentication)")
    public ResponseEntity<ApiResponse<AdvanceReportStatusDTO>> uploadReport(
            @Parameter(description = "Identificador del proyecto") @PathVariable String projectId,
            @Parameter(description = "Archivo del informe de avance (PDF o PPTX)") @RequestPart("file") MultipartFile file,
            Authentication authentication) {

        String pid = projectId.trim().toUpperCase();
        Proyecto proyecto = proyectoRepositoryPort.findById(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String periodo = periodService.currentPeriodo(today);
        String username = identityExtractor.resolveUsername(authentication);

        // Validate file: extensiones con trim y sin entradas vacias (CWE-183)
        Set<String> allowedExtensions = Arrays.stream(
                systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_ALLOWED_EXTENSIONS, "pdf,pptx")
                        .split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
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
        // CWE-434: validar magic bytes reales, no solo el Content-Type declarado por el cliente.
        validateMagicBytes(file, ext);
        if ("pdf".equalsIgnoreCase(ext)) {
            validarPdfReal(file);
        }

        AdvanceReportUpload upload = uploadRepositoryPort.findByProjectIdAndPeriodo(pid, periodo).orElse(null);
        boolean esNuevaCabecera = upload == null;

        int siguienteVersion = esNuevaCabecera ? 1 : versionRepositoryPort.findMaxNumeroVersion(upload.getId()) + 1;
        // CWE-22/CWE-73: base de nombre sin ruta ni caracteres de separacion.
        String safeBaseName = originalFilename
                .substring(0, originalFilename.lastIndexOf('.') > 0 ? originalFilename.lastIndexOf('.') : originalFilename.length())
                .replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedName = String.format("%s_%s_v%d_%s_%s", pid, periodo, siguienteVersion, sanitize(username), safeBaseName);
        String filePath = fileStorageService.storeFile(file, "informes-avance", storedName);

        if (esNuevaCabecera) {
            upload = new AdvanceReportUpload(pid, periodo, originalFilename, filePath, file.getSize(), username);
            upload.setSubidoRol(resolveRol(authentication));
            upload = uploadRepositoryPort.save(upload);
        }

        versionRepositoryPort.findByUploadIdAndEstado(upload.getId(), AdvanceReportVersion.ESTADO_ACTUAL)
                .ifPresent(actual -> {
                    actual.setEstado(AdvanceReportVersion.ESTADO_HISTORICA);
                    versionRepositoryPort.save(actual);
                });

        AdvanceReportVersion version = new AdvanceReportVersion(
                upload.getId(), pid, periodo, siguienteVersion,
                originalFilename, filePath, file.getSize(), file.getContentType(),
                username, resolveRol(authentication));
        versionRepositoryPort.save(version);

        upload.setFileName(originalFilename);
        upload.setFilePath(filePath);
        upload.setFileSize(file.getSize());
        upload.setUploadedBy(username);
        upload.setUploadedAt(LocalDateTime.now(ZoneId.systemDefault()));
        upload.setSubidoRol(resolveRol(authentication));
        upload.setEstado("PENDIENTE");
        upload.setObservaciones(null);
        upload.setVerifiedBy(null);
        upload.setVerifiedAt(null);
        upload.setReturnedBy(null);
        upload.setReturnedAt(null);
        upload = uploadRepositoryPort.save(upload);

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
        } catch (IOException _) {
            throw new BadRequestException("No fue posible validar el archivo PDF cargado.");
        }
    }

    private String resolveRol(Authentication authentication) {
        try {
            return localUserAuthorizationService.requireLocalUser(authentication).getRolCodigo();
        } catch (Exception _) {
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

    /**
     * CWE-434: valida la cabecera binaria real del archivo (magic bytes) segun su extension.
     * El Content-Type del multipart lo declara el cliente y no es confiable.
     */
    private void validateMagicBytes(MultipartFile file, String extension) {
        try (java.io.InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(4);
            boolean valid = switch (extension.toLowerCase()) {
                case "pdf" -> header.length >= 4
                        && header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
                case "pptx", "docx", "xlsx" -> header.length >= 2 && header[0] == 'P' && header[1] == 'K';
                default -> true;
            };
            if (!valid) {
                throw new com.proyecta.api_gestion.domain.exception.BadRequestException(
                        "El contenido del archivo no corresponde a la extension declarada (." + extension + ").");
            }
        } catch (java.io.IOException _) {
            throw new com.proyecta.api_gestion.domain.exception.BadRequestException(
                    "No se pudo validar el contenido del archivo.");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).trim();
    }
}
