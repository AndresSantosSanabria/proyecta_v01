package com.proyecta.api_gestion.service.interfaces;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public interface IStorageProvider {
    String storeFile(MultipartFile file, String subDirectory, String fileName);
    String storeBytes(byte[] content, String subDirectory, String fileName);
    Resource loadFileAsResource(String subDirectory, String fileName);
    boolean fileExists(String subDirectory, String fileName);
    void deleteFile(String subDirectory, String fileName);
    void validateFile(MultipartFile file, long maxSizeBytes, Set<String> allowedMimeTypes);
    String sanitizeFileName(String fileName);
}
