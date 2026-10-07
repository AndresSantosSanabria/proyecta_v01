package com.proyecta.api_gestion.service.support;

import com.proyecta.api_gestion.application.port.out.persistence.config.EstadoEntregableConfigRepositoryPort;
import com.proyecta.api_gestion.domain.exception.BadRequestException;
import com.proyecta.api_gestion.domain.model.Entregable;
import com.proyecta.api_gestion.domain.model.Fase;
import com.proyecta.api_gestion.domain.model.Hito;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.config.EstadoEntregableConfig;
import com.proyecta.api_gestion.dto.proyecto.EntregableDTO;
import com.proyecta.api_gestion.dto.proyecto.FaseDTO;
import com.proyecta.api_gestion.dto.proyecto.HitoDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ProjectStructureSupport {

    private final EstadoEntregableConfigRepositoryPort estadoEntregableConfigRepositoryPort;

    public ProjectStructureSupport(EstadoEntregableConfigRepositoryPort estadoEntregableConfigRepositoryPort) {
        this.estadoEntregableConfigRepositoryPort = estadoEntregableConfigRepositoryPort;
    }

    public Fase buildFase(FaseDTO fDto, Proyecto proyecto, int faseNumero, int[] hitoIdx, int[] entIdx) {
        Fase fase = new Fase();
        fase.setNombre(String.format("F%02d", faseNumero));
        fase.setDescripcion(fDto.descripcion());
        fase.setPonderacion(BigDecimal.valueOf(fDto.ponderacion()));
        fase.setProyecto(proyecto);

        List<Hito> hitos = fDto.hitos().stream()
                .map(hDto -> { hitoIdx[0]++; return buildHito(hDto, fase, hitoIdx[0], entIdx); })
                .toList();
        fase.setHitos(hitos);
        return fase;
    }

    private Hito buildHito(HitoDTO hDto, Fase fase, int hitoNumero, int[] entIdx) {
        Hito hito = new Hito();
        hito.setNombre(String.format("H%02d", hitoNumero));
        hito.setDescripcion(hDto.descripcion());
        hito.setPonderacion(BigDecimal.valueOf(hDto.ponderacion()));
        hito.setFase(fase);

        List<Entregable> entregables = hDto.entregables().stream()
                .map(eDto -> { entIdx[0]++; return buildEntregable(eDto, hito, entIdx[0]); })
                .toList();
        hito.setEntregables(entregables);
        return hito;
    }

    private Entregable buildEntregable(EntregableDTO eDto, Hito hito, int entNumero) {
        validarFechasEntregableNuevo(eDto);
        Entregable ent = new Entregable();
        ent.setNombre(String.format("E%02d", entNumero));
        ent.setDescripcion(eDto.descripcion());
        ent.setPonderacion(BigDecimal.valueOf(eDto.ponderacion()));
        ent.setFechaInicio(eDto.fechaInicio());
        ent.setFechaLimite(eDto.fechaLimite());
        ent.setRetroactivo(eDto.fechaLimite() != null && eDto.fechaLimite().isBefore(LocalDate.now(ZoneId.systemDefault())));
        ent.setArchivoPdf(eDto.archivoPdf());
        ent.setHito(hito);
        ent.setEstadoConfig(estadoEntregableConfig("PENDIENTE"));
        return ent;
    }

    public void validarPonderaciones(List<FaseDTO> fases) {
        if (fases == null || fases.isEmpty()) {
            return;
        }

        int sumFases = 0;
        for (FaseDTO fase : fases) {
            validarPonderacionElemento(fase.ponderacion(), "La ponderacion de la fase debe estar entre 1 y 100");
            sumFases += fase.ponderacion();
        }
        if (sumFases != 100) {
            throw new BadRequestException("La suma de ponderaciones de las fases debe ser exactamente 100");
        }

        for (FaseDTO fase : fases) {
            validarHitosFase(fase);
        }
    }

    private void validarPonderacionElemento(Integer ponderacion, String mensajeError) {
        if (ponderacion == null || ponderacion < 1 || ponderacion > 100) {
            throw new BadRequestException(mensajeError);
        }
    }

    private void validarHitosFase(FaseDTO fase) {
        if (fase.hitos() == null || fase.hitos().isEmpty()) {
            throw new BadRequestException("Una fase debe tener al menos un hito");
        }

        int sumHitos = 0;
        for (HitoDTO hito : fase.hitos()) {
            validarPonderacionElemento(hito.ponderacion(), "La ponderacion del hito debe estar entre 1 y 100");
            sumHitos += hito.ponderacion();
        }
        if (sumHitos != 100) {
            throw new BadRequestException("La suma de ponderaciones de hitos en una fase debe ser exactamente 100");
        }

        for (HitoDTO hito : fase.hitos()) {
            validarEntregablesHito(hito);
        }
    }

    private void validarEntregablesHito(HitoDTO hito) {
        if (hito.entregables() == null || hito.entregables().isEmpty()) {
            throw new BadRequestException("Un hito debe tener al menos un entregable");
        }

        int sumEntregables = 0;
        for (EntregableDTO entregable : hito.entregables()) {
            validarPonderacionElemento(entregable.ponderacion(), "La ponderacion del entregable debe estar entre 1 y 100");
            validarFechasEntregableNuevo(entregable);
            sumEntregables += entregable.ponderacion();
        }
        if (sumEntregables != 100) {
            throw new BadRequestException("La suma de ponderaciones de entregables en un hito debe ser exactamente 100");
        }
    }

    private void validarFechasEntregableNuevo(EntregableDTO entregable) {
        if (entregable.fechaInicio() == null) {
            throw new BadRequestException("La fecha de inicio del entregable es obligatoria");
        }
        if (entregable.fechaLimite() == null) {
            throw new BadRequestException("La fecha limite del entregable es obligatoria");
        }
        if (entregable.fechaLimite().isBefore(entregable.fechaInicio())) {
            throw new BadRequestException("La fecha limite del entregable debe ser mayor o igual a la fecha de inicio del entregable");
        }
    }

    private EstadoEntregableConfig estadoEntregableConfig(String codigo) {
        return estadoEntregableConfigRepositoryPort.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalStateException("Estado de entregable no configurado: " + codigo));
    }
}
