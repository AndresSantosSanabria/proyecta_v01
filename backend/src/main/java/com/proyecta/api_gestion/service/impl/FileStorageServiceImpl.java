package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.InternalErrorException;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;

@Service
public class FileStorageServiceImpl implements IStorageProvider {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    private final SystemParameterService systemParameterService;
    private final Path defaultStorageLocation;

    public FileStorageServiceImpl(SystemParameterService systemParameterService) {
        this.systemParameterService = systemParameterService;
        this.defaultStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.defaultStorageLocation);
        } catch (Exception ex) {
            throw new InternalErrorException("No se pudo crear el directorio de almacenamiento por defecto: " + this.defaultStorageLocation);
        }
    }

    /**
     * Resuelve la ruta padre de almacenamiento.
     * Si el parametro 'storage_base_path' existe en BD y no esta vacio, lo usa.
     * De lo contrario usa la ruta por defecto (uploads/ junto al ejecutable).
     * Valida escritura y protege contra path traversal.
     */
    private Path resolveBaseStorageLocation() {
        String configuredPath = systemParameterService.getString(SystemParameterKeys.STORAGE_BASE_PATH, "").trim();

        Path basePath;
        if (configuredPath.isEmpty()) {
            basePath = defaultStorageLocation;
        } else {
            basePath = Paths.get(configuredPath).toAbsolutePath().normalize();

            if (!basePath.toString().contains("uploads")) {
                logger.warn("La ruta de almacenamiento configurada no contiene 'uploads': {}", basePath);
            }
        }

        ensureDirectoryExists(basePath);
        validateWritability(basePath);

        return basePath;
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory, String fileName) {
        String originalFileName = sanitizeFileName(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = extractExtension(originalFileName);
        String targetFileName = fileName + extension;

        if (targetFileName.contains("..")) {
            throw new BadRequestException("Nombre de archivo invalido: contiene secuencia de ruta prohibida.");
        }

        try {
            Path baseLocation = resolveBaseStorageLocation();
            Path targetDirectory = resolveSubDirectory(baseLocation, subDirectory);
            Path targetLocation = targetDirectory.resolve(targetFileName);

            if (!targetLocation.startsWith(baseLocation)) {
                throw new BadRequestException("Ruta de destino fuera del directorio permitido.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return targetFileName;
        } catch (IOException ex) {
            throw new InternalErrorException("Error al almacenar el archivo: " + targetFileName, ex);
        }
    }

    @Override
    public String storeBytes(byte[] content, String subDirectory, String fileName) {
        String targetFileName = sanitizeFileName(Objects.requireNonNull(fileName));

        if (targetFileName.contains("..")) {
            throw new BadRequestException("Nombre de archivo invalido: contiene secuencia de ruta prohibida.");
        }

        try {
            Path baseLocation = resolveBaseStorageLocation();
            Path targetDirectory = resolveSubDirectory(baseLocation, subDirectory);
            Path targetLocation = targetDirectory.resolve(targetFileName);

            if (!targetLocation.startsWith(baseLocation)) {
                throw new BadRequestException("Ruta de destino fuera del directorio permitido.");
            }

            Files.write(targetLocation, content);
            return targetFileName;
        } catch (IOException ex) {
            throw new InternalErrorException("Error al almacenar el archivo: " + targetFileName, ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String subDirectory, String fileName) {
        try {
            Path baseLocation = resolveBaseStorageLocation();
            Path filePath = resolveSubDirectory(baseLocation, subDirectory).resolve(fileName).normalize();

            if (!filePath.startsWith(baseLocation)) {
                throw new BadRequestException("Ruta de archivo fuera del directorio permitido.");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new InternalErrorException("Archivo no encontrado o no legible: " + fileName);
        } catch (MalformedURLException ex) {
            throw new InternalErrorException("Archivo no encontrado: " + fileName, ex);
        }
    }

    @Override
    public boolean fileExists(String subDirectory, String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        try {
            Path baseLocation = resolveBaseStorageLocation();
            Path filePath = resolveSubDirectory(baseLocation, subDirectory).resolve(fileName).normalize();
            return Files.exists(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void deleteFile(String subDirectory, String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }
        try {
            Path baseLocation = resolveBaseStorageLocation();
            Path filePath = resolveSubDirectory(baseLocation, subDirectory).resolve(fileName).normalize();

            if (!filePath.startsWith(baseLocation)) {
                throw new BadRequestException("Ruta de archivo fuera del directorio permitido.");
            }

            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new InternalErrorException("No se pudo eliminar el archivo: " + fileName, ex);
        }
    }

    @Override
    public void validateFile(MultipartFile file, long maxSizeBytes, Set<String> allowedMimeTypes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo no puede estar vacio.");
        }

        if (file.getSize() > maxSizeBytes) {
            long maxSizeMB = maxSizeBytes / (1024 * 1024);
            throw new BadRequestException("El archivo excede el tamano maximo permitido de " + maxSizeMB + " MB.");
        }

        String mimeType = resolveMimeType(file);
        if (allowedMimeTypes != null && !allowedMimeTypes.isEmpty() && !allowedMimeTypes.contains(mimeType)) {
            throw new BadRequestException("Tipo de archivo no permitido. Tipos aceptados: " + String.join(", ", allowedMimeTypes));
        }
    }

    @Override
    public String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return "";
        }
        String cleaned = StringUtils.cleanPath(fileName);
        if (cleaned.contains("..")) {
            throw new BadRequestException("El nombre del archivo contiene una secuencia de ruta invalida.");
        }
        return cleaned;
    }

    // --- Metodos publicos para validacion externa (controller) ---

    /**
     * Valida si una ruta es valida como base de almacenamiento.
     * Retorna null si es valida, o un mensaje de error si no lo es.
     */
    public String validateStoragePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "La ruta no puede estar vacia.";
        }

        if (path.contains("..")) {
            return "La ruta contiene secuencias de trayectoria no permitidas (..).";
        }

        Path resolved;
        try {
            resolved = Paths.get(path).toAbsolutePath().normalize();
        } catch (Exception e) {
            return "La ruta no es valida: " + e.getMessage();
        }

        Path parent = resolved.getParent();
        if (parent != null && !Files.exists(parent)) {
            return "El directorio padre no existe: " + parent;
        }

        if (Files.exists(resolved) && !Files.isDirectory(resolved)) {
            return "La ruta existe pero no es un directorio.";
        }

        if (Files.exists(resolved)) {
            if (!Files.isWritable(resolved)) {
                return "El directorio no tiene permisos de escritura.";
            }
        }

        return null;
    }

    /**
     * Crea la ruta de almacenamiento y retorna informacion de estado.
     */
    public StoragePathInfo testStoragePath(String path) {
        String error = validateStoragePath(path);
        if (error != null) {
            return new StoragePathInfo(false, error, false, false, false);
        }

        Path resolved = Paths.get(path).toAbsolutePath().normalize();
        boolean exists = Files.exists(resolved);
        boolean writable = exists && Files.isWritable(resolved);
        boolean created = false;

        if (!exists) {
            try {
                Files.createDirectories(resolved);
                created = true;
                writable = Files.isWritable(resolved);
            } catch (IOException e) {
                return new StoragePathInfo(false, "No se pudo crear el directorio: " + e.getMessage(), false, false, false);
            }
        }

        return new StoragePathInfo(true, null, exists, writable, created);
    }

    // --- Metodos privados ---

    private Path resolveSubDirectory(Path baseLocation, String subDirectory) {
        Path targetDir;
        if (subDirectory == null || subDirectory.trim().isEmpty()) {
            targetDir = baseLocation;
        } else {
            String sanitized = sanitizeFileName(subDirectory);
            targetDir = baseLocation.resolve(sanitized).normalize();
        }

        if (!targetDir.startsWith(baseLocation)) {
            throw new BadRequestException("Ruta de almacenamiento fuera del directorio permitido.");
        }

        ensureDirectoryExists(targetDir);
        return targetDir;
    }

    private void ensureDirectoryExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            throw new InternalErrorException("No se pudo crear el directorio: " + dir, ex);
        }
    }

    private void validateWritability(Path dir) {
        try {
            if (Files.exists(dir) && !Files.isWritable(dir)) {
                throw new InternalErrorException("El directorio de almacenamiento no tiene permisos de escritura: " + dir);
            }
        } catch (InternalErrorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalErrorException("Error al verificar permisos del directorio: " + dir, ex);
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex).toLowerCase();
        }
        return "";
    }

    private String resolveMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isEmpty()) {
            return contentType.toLowerCase();
        }

        try {
            String detected = Files.probeContentType(
                    Paths.get(Objects.requireNonNull(file.getOriginalFilename()))
            );
            if (detected != null) {
                return detected.toLowerCase();
            }
        } catch (IOException ignored) {
        }

        String extension = extractExtension(file.getOriginalFilename()).toLowerCase();
        return switch (extension) {
            case ".pdf" -> "application/pdf";
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls" -> "application/vnd.ms-excel";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }

    public record StoragePathInfo(boolean valid, String error, boolean existed, boolean writable, boolean created) {}
}
