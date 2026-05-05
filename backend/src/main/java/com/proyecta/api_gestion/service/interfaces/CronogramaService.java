package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.cronograma.CronogramaResponseDTO;
import com.proyecta.api_gestion.dto.cronograma.CronogramaUploadResponseDTO;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

public interface CronogramaService {
    CronogramaResponseDTO obtenerCronograma(String projectId);
    CronogramaUploadResponseDTO cargarCronograma(String projectId, MultipartFile file);
    Resource descargarCronograma(String projectId);
}
