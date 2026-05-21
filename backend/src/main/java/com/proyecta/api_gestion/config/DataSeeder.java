package com.proyecta.api_gestion.config;

import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.model.enums.*;
import com.proyecta.api_gestion.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DEPRECATED: Esta clase ha sido reemplazada por la nueva arquitectura modular
 * en la carpeta 'service/seed/'.
 * 
 * Se mantiene para referencia histórica.
 * Usar DataInitializer + DataSeederService para nuevas inicializaciones.
 */
@Configuration
@Deprecated(since = "2026-05", forRemoval = true)
public class DataSeeder {

    // @Bean - DESHABILITADO: Usar DataInitializer en su lugar
    // CommandLineRunner initDatabase(
    //         ProyectoRepository proyectoRepository,
    //         PatrocinadorRepository patrocinadorRepository,
    //         UsuarioRepository usuarioRepository,
    //         SystemParameterRepository systemParameterRepository,
    //         ReporteConfigRepository reporteConfigRepository,
    //         RiesgoRepository riesgoRepository
    // ) {
        //return args -> {
        //    System.out.println(">>> SEEDER: Verificando datos...");
        //    ... (código deshabilitado - ver git history si es necesario)
        //    long total = proyectoRepository.count();
        //    System.out.println(">>> SEEDER: Finalizado. Total de proyectos en BD: " + total);
        //};

    // ── Helper: inserta proyecto solo si no existe ──────────────────────────
    private void seedProyecto(
            String id, String nombre, String dependencia, String director, String correo,
            LocalDate fechaInicio, EstadoProyecto estado, String avance, boolean peti,
            Patrocinador patrocinador,
            String faseNombre, String fasePond, String faseAvance,
            String hitoNombre, String entregableNombre, EstadoEntregable entregableEstado, LocalDate fechaLimite,
            String riesgoCodigo, String riesgoDesc, Probabilidad prob, Impacto impacto, NivelRiesgo nivel,
            ProyectoRepository proyectoRepo, RiesgoRepository riesgoRepo) {

        if (proyectoRepo.findById(id).isPresent()) return;

        Proyecto p = new Proyecto();
        p.setId(id);
        p.setNombre(nombre);
        p.setDependencia(dependencia);
        p.setDirector(director);
        p.setCorreoDirector(correo);
        p.setFechaInicio(fechaInicio);
        p.setEstado(estado);
        p.setAvanceTotal(new BigDecimal(avance));
        p.setPeti(peti);
        p.setPatrocinador(patrocinador);

        Fase fase = buildFase(faseNombre, fasePond, faseAvance, p);
        Hito hito = buildHito(hitoNombre, "100.00", fase);
        addEntregable(entregableNombre, "100.00", entregableEstado, fechaLimite, hito);
        p.getFases().add(fase);
        proyectoRepo.save(p);

        if (riesgoCodigo != null) {
            Riesgo r = new Riesgo();
            r.setCodigo(riesgoCodigo);
            r.setDescripcion(riesgoDesc);
            r.setProbabilidad(prob);
            r.setImpacto(impacto);
            r.setNivel(nivel);
            r.setProyecto(p);
            riesgoRepo.save(r);
        }
    }

    private Fase buildFase(String nombre, String ponderacion, String avance, Proyecto proyecto) {
        Fase f = new Fase();
        f.setNombre(nombre);
        f.setPonderacion(new BigDecimal(ponderacion));
        f.setAvanceCalculado(new BigDecimal(avance));
        f.setProyecto(proyecto);
        return f;
    }

    private Hito buildHito(String nombre, String ponderacion, Fase fase) {
        Hito h = new Hito();
        h.setNombre(nombre);
        h.setPonderacion(new BigDecimal(ponderacion));
        h.setFase(fase);
        fase.getHitos().add(h);
        return h;
    }

    private void addEntregable(String nombre, String ponderacion, EstadoEntregable estado,
                               LocalDate fechaLimite, Hito hito) {
        Entregable e = new Entregable();
        e.setNombre(nombre);
        e.setPonderacion(new BigDecimal(ponderacion));
        e.setEstado(estado);
        e.setFechaLimite(fechaLimite);
        e.setHito(hito);
        hito.getEntregables().add(e);
    }
}
