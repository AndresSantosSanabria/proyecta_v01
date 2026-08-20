package com.proyecta.api_gestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.public-url")
public class PublicUrlProperties {

    /**
     * Base pública del aplicativo, por ejemplo https://mi-dominio.com
     */
    private String base;

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }
}
