package com.proyecta.api_gestion.service.seed;

import com.proyecta.api_gestion.model.Usuario;
import com.proyecta.api_gestion.model.enums.Rol;
import com.proyecta.api_gestion.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Seeder especializado para la entidad Usuario.
 * 
 * Responsabilidad: Solo crear/actualizar usuarios (SRP).
 * Patrón: Inyección de dependencias para repository.
 */
@Service
public class UsuarioSeeder {
    
    private static final Logger logger = LoggerFactory.getLogger(UsuarioSeeder.class);
    private final UsuarioRepository usuarioRepository;

    public UsuarioSeeder(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public void seedUsuarios() {
        logger.info("Cargando usuarios semilla...");
        
        crearUsuarioSiNoExiste(
            "admin@proyecta.com",
            "Administrador Proyecta",
            "hash_simulado",
            Rol.ADMINISTRADOR
        );
        
        crearUsuarioSiNoExiste(
            "gestor@proyecta.com",
            "Gestor de Proyectos",
            "hash_simulado",
            Rol.GESTOR_PROYECTOS
        );
        
        crearUsuarioSiNoExiste(
            "analista@proyecta.com",
            "Analista de Proyectos",
            "hash_simulado",
            Rol.ANALISTA_PROYECTOS
        );
        
        logger.info("✓ Usuarios semilla cargados");
    }

    private void crearUsuarioSiNoExiste(String correo, String nombre, String hash, Rol rol) {
        if (usuarioRepository.findByCorreo(correo).isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setNombre(nombre);
            usuario.setCorreo(correo);
            usuario.setContrasenaHash(hash);
            usuario.setRol(rol);
            usuario.setActivo(true);
            usuarioRepository.save(usuario);
            logger.debug("Usuario creado: {}", correo);
        }
    }
}
