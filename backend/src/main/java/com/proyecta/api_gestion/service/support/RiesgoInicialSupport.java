package com.proyecta.api_gestion.service.support;

import com.proyecta.api_gestion.application.port.out.persistence.RiesgoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.config.MatrizRiesgoRepositoryPort;
import com.proyecta.api_gestion.domain.exception.BadRequestException;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.Riesgo;
import com.proyecta.api_gestion.domain.model.config.MatrizRiesgo;
import com.proyecta.api_gestion.domain.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.domain.model.enums.Impacto;
import com.proyecta.api_gestion.domain.model.enums.NivelRiesgo;
import com.proyecta.api_gestion.domain.model.enums.Probabilidad;
import com.proyecta.api_gestion.domain.model.enums.TipoRiesgo;
import com.proyecta.api_gestion.dto.proyecto.RiesgoCompletitudDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiesgoInicialSupport {

    private final RiesgoRepositoryPort riesgoRepositoryPort;
    private final MatrizRiesgoRepositoryPort matrizRiesgoRepositoryPort;

    public RiesgoInicialSupport(RiesgoRepositoryPort riesgoRepositoryPort,
                                MatrizRiesgoRepositoryPort matrizRiesgoRepositoryPort) {
        this.riesgoRepositoryPort = riesgoRepositoryPort;
        this.matrizRiesgoRepositoryPort = matrizRiesgoRepositoryPort;
    }

    public void crearRiesgosIniciales(Proyecto proyecto, List<RiesgoCompletitudDTO> riesgos) {
        if (riesgos == null || riesgos.isEmpty()) {
            return;
        }
        for (RiesgoCompletitudDTO dto : riesgos) {
            Riesgo riesgo = new Riesgo();
            riesgo.setProyecto(proyecto);
            riesgo.setDescripcion(dto.descripcion().trim());
            riesgo.setProbabilidad(dto.probabilidad());
            riesgo.setImpacto(dto.impacto());
            riesgo.setTipoRiesgo(dto.tipoRiesgo() != null ? dto.tipoRiesgo() : TipoRiesgo.GENERAL);
            riesgo.setNivel(parseNivelRiesgo(calcularNivelRiesgo(dto.probabilidad(), dto.impacto())));
            riesgo.setEntidadResponsable(dto.entidadResponsable() != null ? dto.entidadResponsable().trim() : null);
            riesgo.setAccionesMitigacion(null);
            riesgo.setFechaAccion(null);
            riesgo.setEstado(EstadoRiesgo.PENDIENTE);

            Riesgo saved = riesgoRepositoryPort.save(riesgo);
            saved.setCodigo("R" + String.format("%02d", saved.getId()));
            riesgoRepositoryPort.save(saved);
        }
    }

    private String calcularNivelRiesgo(Probabilidad prob, Impacto imp) {
        String probabilidad = prob == null ? null : prob.name();
        String impacto = imp == null ? null : imp.name();
        if (probabilidad == null || impacto == null) {
            throw new BadRequestException("La probabilidad e impacto son obligatorios para calcular el nivel de riesgo.");
        }
        return matrizRiesgoRepositoryPort.findByProbabilidadIgnoreCaseAndImpactoIgnoreCase(probabilidad, impacto)
                .map(MatrizRiesgo::getNivelResultante)
                .orElseGet(() -> {
                    int p = escalaProbabilidad(probabilidad);
                    int i = escalaImpacto(impacto);
                    int score = p + i;
                    if (score >= 2 && score <= 4) return "BAJO";
                    if (score >= 5 && score <= 6) return "MODERADO";
                    if (score >= 7 && score <= 8) return "ALTO";
                    if (score >= 9) return "EXTREMO";
                    return "BAJO";
                });
    }

    private int escalaProbabilidad(String probabilidad) {
        return switch (probabilidad.toUpperCase()) {
            case "UNO" -> 1;
            case "DOS" -> 2;
            case "TRES" -> 3;
            case "CUATRO" -> 4;
            case "CINCO" -> 5;
            default -> 0;
        };
    }

    private int escalaImpacto(String impacto) {
        return switch (impacto.toUpperCase()) {
            case "UNO" -> 1;
            case "DOS" -> 2;
            case "TRES" -> 3;
            case "CUATRO" -> 4;
            case "CINCO" -> 5;
            default -> 0;
        };
    }

    private NivelRiesgo parseNivelRiesgo(String nivel) {
        try {
            return NivelRiesgo.valueOf(nivel);
        } catch (IllegalArgumentException _) {
            return NivelRiesgo.BAJO;
        }
    }
}
