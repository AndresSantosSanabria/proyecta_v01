package com.proyecta.api_gestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.proyecta.api_gestion")
@EnableScheduling
public class ApiGestionApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGestionApplication.class, args);
	}

}
