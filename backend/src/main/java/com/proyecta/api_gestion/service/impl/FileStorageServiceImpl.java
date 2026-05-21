package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.InternalErrorException;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;

@Service
public class FileStorageServiceImpl implements IStorageProvider {

    private final Path baseStorageLocation;

    public FileStorageServiceImpl() {
        this.baseStorageLocation = Paths.get("uploads")
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.baseStorageLocation);
        } catch (Exception ex) {
            throw new InternalErrorException("No se pudo crear el directorio de almacenamiento: " + this.baseStorageLocation);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory, String fileName) {
        String originalFileName = sanitizeFileName(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = extractExtension(originalFileName);
        String targetFileName = fileName + extension;

        if (targetFileName.contains("..")) {
            throw new BadRequestException("Nombre de archivo inválido: contiene secuencia de ruta prohibida.");
        }

        try {
            Path targetDirectory = resolveSubDirectory(subDirectory);
            Path targetLocation = targetDirectory.resolve(targetFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return targetFileName;
        } catch (IOException ex) {
            throw new InternalErrorException("Error al almacenar el archivo: " + targetFileName, ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String subDirectory, String fileName) {
        try {
            Path filePath = resolveSubDirectory(subDirectory).resolve(fileName).normalize();
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
            Path filePath = resolveSubDirectory(subDirectory).resolve(fileName).normalize();
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
            Path filePath = resolveSubDirectory(subDirectory).resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new InternalErrorException("No se pudo eliminar el archivo: " + fileName, ex);
        }
    }

    @Override
    public void validateFile(MultipartFile file, long maxSizeBytes, Set<String> allowedMimeTypes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo no puede estar vacío.");
        }

        if (file.getSize() > maxSizeBytes) {
            long maxSizeMB = maxSizeBytes / (1024 * 1024);
            throw new BadRequestException("El archivo excede el tamaño máximo permitido de " + maxSizeMB + " MB.");
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
            throw new BadRequestException("El nombre del archivo contiene una secuencia de ruta inválida.");
        }
        return cleaned;
    }

    private Path resolveSubDirectory(String subDirectory) {
        Path targetDir;
        if (subDirectory == null || subDirectory.trim().isEmpty()) {
            targetDir = baseStorageLocation;
        } else {
            String sanitized = sanitizeFileName(subDirectory);
            targetDir = baseStorageLocation.resolve(sanitized).normalize();
        }

        if (!targetDir.startsWith(baseStorageLocation)) {
            throw new BadRequestException("Ruta de almacenamiento fuera del directorio permitido.");
        }

        try {
            Files.createDirectories(targetDir);
        } catch (IOException ex) {
            throw new InternalErrorException("No se pudo crear el directorio: " + targetDir, ex);
        }

        return targetDir;
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
}
