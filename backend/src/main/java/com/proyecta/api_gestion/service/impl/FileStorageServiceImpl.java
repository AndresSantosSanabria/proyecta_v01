package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.exception.InternalErrorException;
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

@Service
public class FileStorageServiceImpl {

    private final Path fileStorageLocation;

    public FileStorageServiceImpl() {
        this.fileStorageLocation = Paths.get("uploads/schedules")
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new InternalErrorException("No se pudo crear el directorio donde se almacenarán los archivos subidos.");
        }
    }

    public String storeFile(MultipartFile file, String fileName) {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }
        
        String targetFileName = fileName + extension;

        try {
            if (targetFileName.contains("..")) {
                throw new InternalErrorException("El nombre del archivo contiene una secuencia de ruta inválida " + targetFileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(targetFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return targetFileName;
        } catch (IOException ex) {
            throw new InternalErrorException("No se pudo almacenar el archivo " + targetFileName + ". ¡Por favor, inténtelo de nuevo!");
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new InternalErrorException("Archivo no encontrado " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new InternalErrorException("Archivo no encontrado " + fileName);
        }
    }
}
