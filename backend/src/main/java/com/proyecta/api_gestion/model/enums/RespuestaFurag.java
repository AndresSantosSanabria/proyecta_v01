package com.proyecta.api_gestion.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum RespuestaFurag {
    SI("SI"),
    NO("NO"),
    NA("NA"),
    NO_APLICA("NO_APLICA");

    private final String value;

    RespuestaFurag(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RespuestaFurag fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "SI", "S" -> SI;
            case "NO", "N" -> NO;
            case "NA" -> NA;
            case "NO_APLICA", "N/A", "NOAPLICA" -> NO_APLICA;
            default -> throw new IllegalArgumentException("Valor FURAG inválido: " + value);
        };
    }
}
