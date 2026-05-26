package com.proyecta.api_gestion.service.security.dynamic;

import com.proyecta.api_gestion.dto.security.SeguridadUsuarioProyectoRequest;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.exception.ForbiddenException;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.SystemParameter;
import com.proyecta.api_gestion.model.security.SeguridadRol;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.model.security.SeguridadUsuarioProyecto;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.SystemParameterRepository;
import com.proyecta.api_gestion.repository.security.SeguridadPermisoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadRolPermisoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadRolRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAdministrationServiceTest {

    @Mock
    private SeguridadUsuarioRepository usuarioRepository;
    @Mock
    private SeguridadRolRepository rolRepository;
    @Mock
    private SeguridadPermisoRepository permisoRepository;
    @Mock
    private SeguridadRolPermisoRepository rolPermisoRepository;
    @Mock
    private SeguridadUsuarioProyectoRepository usuarioProyectoRepository;
    @Mock
    private ProyectoRepository proyectoRepository;
    @Mock
    private SystemParameterRepository systemParameterRepository;
    @Mock
    private SystemParameterService systemParameterService;
    @Mock
    private SecurityCatalogCacheService catalogCacheService;
    @Mock
    private KeycloakIdentityExtractor identityExtractor;
    @Mock
    private LocalUserAuthorizationService localUserAuthorizationService;

    private SecurityAdministrationService service;

    @BeforeEach
    void setUp() {
        service = new SecurityAdministrationService(
                usuarioRepository,
                rolRepository,
                permisoRepository,
                rolPermisoRepository,
                usuarioProyectoRepository,
                proyectoRepository,
                systemParameterRepository,
                systemParameterService,
                catalogCacheService,
                identityExtractor,
                localUserAuthorizationService);
    }

    @Test
    void asignarUsuarioProyecto_rechazaCargoNoConfigurado() {
        when(systemParameterService.getCsv("seguridad_cargos_asignacion", List.of()))
                .thenReturn(List.of("DIRECTOR_PROYECTO", "ANALISTA"));

        SeguridadUsuarioProyectoRequest request = new SeguridadUsuarioProyectoRequest(
                "fabio",
                "IS-PROY-001",
                "APOYO");

        assertThrows(BadRequestException.class, () -> service.asignarUsuarioProyecto(request));
    }

    @Test
    void listarAsignaciones_enriqueceProyectoLegible() {
        SeguridadUsuario usuario = new SeguridadUsuario();
        usuario.setId(1L);
        usuario.setUsername("fabio");
        usuario.setNombre("Fabio Andres");

        SeguridadUsuarioProyecto assignment = new SeguridadUsuarioProyecto();
        assignment.setId(10L);
        assignment.setUsuario(usuario);
        assignment.setProyectoId("IS-PROY-001");
        assignment.setCargo("DIRECTOR_PROYECTO");
        assignment.setActivo(true);

        Proyecto proyecto = new Proyecto();
        proyecto.setId("IS-PROY-001");
        proyecto.setNombre("Modernizacion del Data Center Principal");

        when(usuarioProyectoRepository.findByUsername("fabio")).thenReturn(List.of(assignment));
        when(proyectoRepository.findById("IS-PROY-001")).thenReturn(Optional.of(proyecto));

        var result = service.listarAsignaciones("fabio");

        assertEquals(1, result.size());
        assertEquals("IS-PROY-001", result.get(0).proyectoId());
        assertEquals("IS-PROY-001", result.get(0).proyectoCodigo());
        assertEquals("Modernizacion del Data Center Principal", result.get(0).proyectoNombre());
    }

    @Test
    void eliminarRol_bloqueaRolesBase() {
        SeguridadRol rol = new SeguridadRol();
        rol.setId(1L);
        rol.setCodigo("admin");
        rol.setNombre("Administrador");
        rol.setActivo(true);

        when(rolRepository.findByCodigoIgnoreCase("admin")).thenReturn(Optional.of(rol));

        assertThrows(ForbiddenException.class, () -> service.eliminarRol("admin"));
    }
}
