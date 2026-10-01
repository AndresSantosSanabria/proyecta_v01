package com.proyecta.api_gestion.service.seed;

import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import com.proyecta.api_gestion.repository.security.SeguridadRolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class UsuarioSeeder {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioSeeder.class);
    private static final String ROL_ADMIN = "admin";

    private final SeguridadUsuarioRepository seguridadUsuarioRepository;
    private final List<UsuarioSeed> configuredUsers;
    private final String adminUsername;
    private final String adminCorreo;
    private final String adminNombre;

    public UsuarioSeeder(
            SeguridadUsuarioRepository seguridadUsuarioRepository,
            SeguridadRolRepository seguridadRolRepository,
            @Value("${gob.seed.users:}") String seedUsers,
            @Value("${gob.seed.admin.username:}") String adminUsername,
            @Value("${gob.seed.admin.correo:}") String adminCorreo,
            @Value("${gob.seed.admin.nombre:}") String adminNombre) {
        this.seguridadUsuarioRepository = seguridadUsuarioRepository;
        this.configuredUsers = parseUsers(seedUsers);
        this.adminUsername = trimToNull(adminUsername);
        this.adminCorreo = trimToNull(adminCorreo);
        this.adminNombre = adminNombre;
    }

    public void seedUsuarios() {
        ensurePermanentAdmin();

        if (configuredUsers.isEmpty()) {
            logger.info("No hay usuarios semilla configurados en gob.seed.users.");
            return;
        }

        logger.info("Cargando {} usuarios semilla configurados...", configuredUsers.size());
        configuredUsers.forEach(this::upsertUsuario);
        logger.info("Usuarios semilla cargados desde configuracion");
    }

    /**
     * Crea y fuerza a rol admin la cuenta inicial en cada arranque (requerida en el
     * despliegue). El usuario/correo/nombre NO estan en el codigo: vienen de
     * gob.seed.admin.* (propiedades, sobrecribibles por GOB_SEED_ADMIN_*).
     * Para desactivar este seed en un entorno: GOB_SEED_ADMIN_USERNAME= (vacio).
     * Otras cuentas administrativas se declaran en GOB_SEED_USERS.
     */
    private void ensurePermanentAdmin() {
        if (adminUsername == null) {
            logger.debug("GOB_SEED_ADMIN_USERNAME no definido: se omite la creacion de cuenta admin permanente.");
            return;
        }

        SeguridadUsuario usuario = seguridadUsuarioRepository
                .findByUsernameIgnoreCase(adminUsername)
                .or(() -> adminCorreo != null
                        ? seguridadUsuarioRepository.findByCorreoIgnoreCase(adminCorreo)
                        : java.util.Optional.empty())
                .orElseGet(SeguridadUsuario::new);

        boolean isNew = usuario.getId() == null;
        usuario.setUsername(adminUsername);
        if (adminNombre != null && !adminNombre.isBlank()) {
            usuario.setNombre(adminNombre);
        }
        if (adminCorreo != null && !adminCorreo.isBlank()) {
            usuario.setCorreo(adminCorreo);
            if (usuario.getKeycloakSub() == null || usuario.getKeycloakSub().isBlank()) {
                usuario.setKeycloakSub(adminCorreo);
            }
        }
        usuario.setActivo(true);
        usuario.setRolCodigo(ROL_ADMIN);
        usuario.setRolNombre("Administrador");
        seguridadUsuarioRepository.save(usuario);
        logger.info("Usuario admin '{}' forzado a rol admin ({})", adminUsername,
                isNew ? "creado" : "actualizado");
    }

    private void upsertUsuario(UsuarioSeed seed) {
        SeguridadUsuario usuario = seguridadUsuarioRepository.findByCorreoIgnoreCase(seed.correo())
                .orElseGet(SeguridadUsuario::new);
        boolean isNew = usuario.getId() == null;

        usuario.setNombre(seed.nombre());
        usuario.setCorreo(seed.correo());
        usuario.setUsername(seed.correo());
        usuario.setKeycloakSub(firstNonBlank(seed.keycloakSub(), seed.correo()));
        usuario.setActivo(true);

        String rolCodigo = seed.adminLocal() ? ROL_ADMIN : mapRolToCodigo(seed.rol());
        usuario.setRolCodigo(rolCodigo);
        usuario.setRolNombre(rolCodigo);

        seguridadUsuarioRepository.save(usuario);
        logger.debug("Usuario {} desde configuracion: {}", isNew ? "creado" : "actualizado", seed.correo());
    }

    private String mapRolToCodigo(String rolName) {
        if (rolName == null) return "visualizador";
        String lower = rolName.toLowerCase(Locale.ROOT).trim();
        return switch (lower) {
            case "administrador", ROL_ADMIN -> ROL_ADMIN;
            case "gestor_proyectos_ti", "gestor_tic" -> "gestor_tic";
            case "gestor_proyectos", "gestor_pro", "gestor_proyecto" -> "gestor_proyectos";
            case "director_proyecto", "director_pro", "director" -> "director_proyecto";
            case "auditor" -> "auditor";
            case "consulta", "analista" -> "consulta";
            default -> "visualizador";
        };
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
        String rol = requirePart(parts, 2, "rol");
        boolean adminLocal = Boolean.parseBoolean(requirePart(parts, 3, "adminLocal"));
        String keycloakSub = parts.length > 4 ? trimToNull(parts[4]) : null;

        return new UsuarioSeed(correo, nombre, rol, adminLocal, keycloakSub);
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

    private record UsuarioSeed(String correo, String nombre, String rol, boolean adminLocal, String keycloakSub) {}
}
