package com.proyecta.api_gestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.proyecta.api_gestion.config.PublicUrlProperties;

@SpringBootApplication(scanBasePackages = "com.proyecta.api_gestion")
@EnableConfigurationProperties(PublicUrlProperties.class)
@EnableScheduling
@EnableAsync
public class ApiGestionApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGestionApplication.class, args);
	}

}
