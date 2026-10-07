package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.avance.IndicadoresEficienciaDTO;

import java.time.LocalDate;

public interface IIndicadorEficienciaService {
    IndicadoresEficienciaDTO calcular(String proyectoId, LocalDate fechaCorte);
}
