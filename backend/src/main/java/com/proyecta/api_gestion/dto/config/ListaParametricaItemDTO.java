package com.proyecta.api_gestion.dto.config;

public record ListaParametricaItemDTO(
        Long id,
        String listaClave,
        String itemCodigo,
        String itemNombre,
        Integer orden,
        Boolean activo,
        String listaNombreCampo,
        String listaDescripcion,
        String listaTipo
) {}
