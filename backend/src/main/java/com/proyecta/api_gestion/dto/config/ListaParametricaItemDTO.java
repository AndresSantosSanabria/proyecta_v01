package com.proyecta.api_gestion.dto.config;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"id":4101,"listaClave":"TIPO_DOCUMENTO","itemCodigo":"VIABILIZACION","itemNombre":"Informe de viabilidad","orden":1,"activo":true,"listaNombreCampo":"Tipo de documento","listaDescripcion":"Tipos de documento admitidos en el proyecto","listaTipo":"ESTATICA"}
    """)
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
