package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.document.DocumentoInternoDTO;
import com.proyecta.api_gestion.dto.document.DocumentoInternoDownload;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public interface IDocumentoInternoService {

    DocumentoInternoDTO cargar(
            String nombre,
            String descripcion,
            LocalDate fechaCreacion,
            MultipartFile archivo,
            Authentication authentication);

    DocumentoInternoDTO.Listado listar(String nombre, Integer anio, String descripcion, Integer page, Integer size);

    DocumentoInternoDownload descargar(Long id);

    DocumentoInternoDownload ver(Long id);
}
