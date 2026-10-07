package com.proyecta.api_gestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.frontend-url")
public class FrontendUrlProperties {

    /**
     * Base publica del frontend, por ejemplo https://proyecta.cundinamarca.gov.co
     */
    private String base;

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }
}
