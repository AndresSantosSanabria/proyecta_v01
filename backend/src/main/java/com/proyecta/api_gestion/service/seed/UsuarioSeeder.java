package com.proyecta.api_gestion.service.seed;

import com.proyecta.api_gestion.model.Usuario;
import com.proyecta.api_gestion.model.config.RolConfig;
import com.proyecta.api_gestion.model.enums.Rol;
import com.proyecta.api_gestion.repository.UsuarioRepository;
import com.proyecta.api_gestion.repository.config.RolConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Seeder especializado para usuarios locales.
 *
 * La lista se recibe por configuracion para evitar identidades quemadas en codigo.
 */
@Service
public class UsuarioSeeder {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioSeeder.class);
    private final UsuarioRepository usuarioRepository;
    private final RolConfigRepository rolConfigRepository;
    private final List<UsuarioSeed> configuredUsers;

    public UsuarioSeeder(
            UsuarioRepository usuarioRepository,
            RolConfigRepository rolConfigRepository,
            @Value("${gob.seed.users:}") String seedUsers) {
        this.usuarioRepository = usuarioRepository;
        this.rolConfigRepository = rolConfigRepository;
        this.configuredUsers = parseUsers(seedUsers);
    }

    public void seedUsuarios() {
        seedDefaultRoles();

        if (configuredUsers.isEmpty()) {
            logger.info("No hay usuarios semilla configurados en gob.seed.users.");
            return;
        }

        logger.info("Cargando {} usuarios semilla configurados...", configuredUsers.size());
        configuredUsers.forEach(this::upsertUsuario);
        logger.info("Usuarios semilla cargados desde configuracion");
    }

    private void seedDefaultRoles() {
        rolConfigRepository.findByCodigo("ADMINISTRADOR").orElseGet(() -> {
            RolConfig nuevo = new RolConfig();
            nuevo.setCodigo("ADMINISTRADOR");
            nuevo.setNombre("Administrador");
            nuevo.setDescripcion("Acceso total al sistema");
            nuevo.setNivelAcceso(100);
            nuevo.setActivo(true);
            return rolConfigRepository.save(nuevo);
        });

        rolConfigRepository.findByCodigo("VISUALIZADOR").orElseGet(() -> {
            RolConfig nuevo = new RolConfig();
            nuevo.setCodigo("VISUALIZADOR");
            nuevo.setNombre("Visualizador");
            nuevo.setDescripcion("Acceso de solo lectura por defecto");
            nuevo.setNivelAcceso(10);
            nuevo.setActivo(true);
            return rolConfigRepository.save(nuevo);
        });
    }

    private void upsertUsuario(UsuarioSeed seed) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(seed.correo()).orElseGet(Usuario::new);
        boolean isNew = usuario.getId() == null;

        usuario.setNombre(seed.nombre());
        usuario.setCorreo(seed.correo());
        usuario.setKeycloakSub(firstNonBlank(seed.keycloakSub(), seed.correo()));
        usuario.setContrasenaHash(null);
        usuario.setRol(seed.rol());
        usuario.setActivo(true);

        if (seed.adminLocal()) {
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
        logger.debug("Usuario {} desde configuracion: {}", isNew ? "creado" : "actualizado", seed.correo());
    }

    private List<UsuarioSeed> parseUsers(String seedUsers) {
        String cleanConfig = trimToNull(seedUsers);
        if (cleanConfig == null) {
            return List.of();
        }

        return Arrays.stream(cleanConfig.split(";"))
                .map(this::parseUser)
                .toList();
    }

    private UsuarioSeed parseUser(String rawUser) {
        String[] parts = rawUser.split("\\|", -1);
        if (parts.length < 4) {
            throw new IllegalArgumentException(
                    "Formato invalido para gob.seed.users. Use correo|nombre|rol|adminLocal|keycloakSub opcional");
        }

        String correo = requirePart(parts, 0, "correo");
        String nombre = requirePart(parts, 1, "nombre");
        Rol rol = parseRol(requirePart(parts, 2, "rol"));
        boolean adminLocal = Boolean.parseBoolean(requirePart(parts, 3, "adminLocal"));
        String keycloakSub = parts.length > 4 ? trimToNull(parts[4]) : null;

        return new UsuarioSeed(correo, nombre, rol, adminLocal, keycloakSub);
    }

    private Rol parseRol(String value) {
        try {
            return Rol.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Rol invalido en gob.seed.users: " + value, ex);
        }
    }

    private String requirePart(String[] parts, int index, String name) {
        String value = index < parts.length ? trimToNull(parts[index]) : null;
        if (value == null) {
            throw new IllegalArgumentException("Falta " + name + " en gob.seed.users");
        }
        return value;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private record UsuarioSeed(String correo, String nombre, Rol rol, boolean adminLocal, String keycloakSub) {}
}
