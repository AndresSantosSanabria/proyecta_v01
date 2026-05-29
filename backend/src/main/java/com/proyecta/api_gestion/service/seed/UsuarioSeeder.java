package com.proyecta.api_gestion.service.seed;

import com.proyecta.api_gestion.model.Usuario;
import com.proyecta.api_gestion.model.config.RolConfig;
import com.proyecta.api_gestion.model.enums.Rol;
import com.proyecta.api_gestion.repository.UsuarioRepository;
import com.proyecta.api_gestion.repository.config.RolConfigRepository;
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
    private final RolConfigRepository rolConfigRepository;

    public UsuarioSeeder(UsuarioRepository usuarioRepository, RolConfigRepository rolConfigRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolConfigRepository = rolConfigRepository;
    }

    public void seedUsuarios() {
        logger.info("Cargando usuarios semilla...");


        upsertUsuario(
                "gestor@proyecta.com",
                "Gestor de Proyectos",
                "hash_simulado",
                Rol.GESTOR_PROYECTOS,
                false);

        upsertUsuario(
                "analista@proyecta.com",
                "Analista de Proyectos",
                "hash_simulado",
                Rol.ANALISTA_PROYECTOS,
                false);

        upsertUsuario(
                "fabio.santos@cundinamarca.gov.co",
                "Fabio Santos",
                "hash_simulado",
                Rol.ADMINISTRADOR,
                true);

        logger.info("✓ Usuarios semilla cargados");
    }

    private void upsertUsuario(String correo, String nombre, String hash, Rol rol, boolean adminLocal) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo).orElseGet(Usuario::new);
        boolean isNew = usuario.getId() == null;

        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setKeycloakSub(correo);
        usuario.setContrasenaHash(hash);
        usuario.setRol(rol);
        usuario.setActivo(true);

        if (adminLocal) {
            RolConfig adminRole = rolConfigRepository.findByCodigo("ADMINISTRADOR").orElseGet(() -> {
                RolConfig nuevo = new RolConfig();
                nuevo.setCodigo("ADMINISTRADOR");
                nuevo.setNombre("Administrador");
                nuevo.setDescripcion("Acceso total al sistema");
                nuevo.setNivelAcceso(100);
                nuevo.setActivo(true);
                return rolConfigRepository.save(nuevo);
            });
            usuario.setRolConfig(adminRole);
        }

        usuarioRepository.save(usuario);
        logger.debug("Usuario {}: {}", isNew ? "creado" : "actualizado", correo);
    }
}
