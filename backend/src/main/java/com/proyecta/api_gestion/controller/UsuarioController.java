package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IUsuarioController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.user.UsuarioDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController implements IUsuarioController {

    @Override
    public ResponseEntity<ApiResponse<UsuarioDTO>> getMe() {
        // Mock de usuario para propósitos de demostración. 
        // En una implementación real, esto se obtendría del SecurityContextHolder.
        UsuarioDTO mockUser = new UsuarioDTO(
            "Administrador de Pruebas",
            "admin@proyecta.gov.co",
            "ADMINISTRADOR",
            "Secretaría de TIC"
        );
        return ResponseEntity.ok(ApiResponse.success(mockUser, "Perfil de usuario recuperado"));
    }
}
