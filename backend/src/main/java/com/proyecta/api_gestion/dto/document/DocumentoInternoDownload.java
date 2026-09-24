package com.proyecta.api_gestion.dto.document;

import org.springframework.core.io.Resource;

public record DocumentoInternoDownload(Resource resource, String filename, String mimeType) {
}
