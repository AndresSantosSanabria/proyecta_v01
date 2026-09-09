package com.proyecta.api_gestion.model.enums;

public enum DetailMode {
    RESUMIDO,
    DETALLADO;

    public static DetailMode from(String value) {
        if (value != null && value.trim().equalsIgnoreCase("detallado")) {
            return DETALLADO;
        }
        return RESUMIDO;
    }

    public boolean isDetailed() {
        return this == DETALLADO;
    }
}
