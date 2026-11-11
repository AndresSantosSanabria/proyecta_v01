package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.application.port.out.persistence.RiesgoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.RiesgoTratamientoAdjuntoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.RiesgoTratamientoRepositoryPort;
import com.proyecta.api_gestion.domain.exception.BadRequestException;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.Riesgo;
import com.proyecta.api_gestion.domain.model.enums.EstadoRiesgo;
import com.proyecta.api_gestion.dto.risk.RiesgoTratamientoDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoTratamientoRequest;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cada tratamiento de riesgo debe adjuntar al menos un PDF y no mas de diez.
 * La regla se valida antes de persistir nada, para no dejar tratamientos incompletos.
 */
class RiesgoTratamientoServiceImplTest {

    private static final String PROJECT_ID = "PROY-CUN-2026-003";
    private static final Integer RIESGO_ID = 7;

    private RiesgoTratamientoRepositoryPort tratamientos;
    private RiesgoTratamientoAdjuntoRepositoryPort adjuntos;
    private RiesgoTratamientoServiceImpl service;

    @BeforeEach
    void setUp() {
        tratamientos = mock(RiesgoTratamientoRepositoryPort.class);
        adjuntos = mock(RiesgoTratamientoAdjuntoRepositoryPort.class);
        RiesgoRepositoryPort riesgos = mock(RiesgoRepositoryPort.class);
        IStorageProvider storage = mock(IStorageProvider.class);
        NotificationEventPublisherPort notificador = mock(NotificationEventPublisherPort.class);

        Proyecto proyecto = new Proyecto();
        proyecto.setId(PROJECT_ID);

        Riesgo riesgo = new Riesgo();
        riesgo.setId(RIESGO_ID);
        riesgo.setProyecto(proyecto);
        // Estado ya tratado: evita disparar la notificacion dentro del caso de prueba.
        riesgo.setEstado(EstadoRiesgo.TRATADO);

        when(riesgos.findById(RIESGO_ID)).thenReturn(Optional.of(riesgo));
        when(tratamientos.countByRiesgoId(RIESGO_ID)).thenReturn(0L);
        when(tratamientos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(adjuntos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.sanitizeFileName(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.storeFile(any(), any(), any())).thenReturn("riesgos-tratamientos/doc.pdf");

        service = new RiesgoTratamientoServiceImpl(tratamientos, adjuntos, riesgos, storage, notificador);
    }

    @Test
    void rechazaElTratamientoSinArchivosPdf() {
        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.crearTratamiento(PROJECT_ID, RIESGO_ID,
                        new RiesgoTratamientoRequest("Mitigacion aplicada"), null));

        assertTrue(error.getMessage().contains("al menos 1 archivo PDF"), error.getMessage());
        verify(tratamientos, never()).save(any());
    }

    @Test
    void rechazaMasDeDiezArchivosPorTratamiento() {
        MultipartFile[] once = new MultipartFile[11];
        for (int i = 0; i < once.length; i++) {
            once[i] = pdf(i);
        }

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.crearTratamiento(PROJECT_ID, RIESGO_ID,
                        new RiesgoTratamientoRequest("Mitigacion aplicada"), once));

        assertTrue(error.getMessage().contains("No se pueden adjuntar más de 10"), error.getMessage());
        verify(tratamientos, never()).save(any());
    }

    @Test
    void aceptaUnSoloArchivoPdfComoMinimo() {
        MultipartFile[] uno = { pdf(0) };

        RiesgoTratamientoDTO dto = service.crearTratamiento(PROJECT_ID, RIESGO_ID,
                new RiesgoTratamientoRequest("Mitigacion aplicada"), uno);

        assertNotNull(dto);
        assertEquals("Mitigacion aplicada", dto.comentario());
        verify(adjuntos, times(1)).save(any());
        verify(tratamientos, times(1)).save(any());
    }

    @Test
    void aceptaHastaDiezArchivos() {
        MultipartFile[] diez = new MultipartFile[10];
        for (int i = 0; i < diez.length; i++) {
            diez[i] = pdf(i);
        }

        RiesgoTratamientoDTO dto = service.crearTratamiento(PROJECT_ID, RIESGO_ID,
                new RiesgoTratamientoRequest("Mitigacion aplicada"), diez);

        assertNotNull(dto);
        verify(adjuntos, times(10)).save(any());
    }

    @Test
    void rechazaArchivosQueNoSonPdf() {
        MultipartFile[] noPdf = {
                new MockMultipartFile("archivos", "hoja.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "contenido".getBytes(StandardCharsets.US_ASCII))
        };

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.crearTratamiento(PROJECT_ID, RIESGO_ID,
                        new RiesgoTratamientoRequest("Mitigacion aplicada"), noPdf));

        assertNotNull(error.getMessage());
        verify(adjuntos, never()).save(any());
    }

    private MultipartFile pdf(int index) {
        return new MockMultipartFile("archivos", "documento_" + index + ".pdf", "application/pdf",
                ("%PDF-1.4\ncontenido del documento " + index).getBytes(StandardCharsets.US_ASCII));
    }
}
