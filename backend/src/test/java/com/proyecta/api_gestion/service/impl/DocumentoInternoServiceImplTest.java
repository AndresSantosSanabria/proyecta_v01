package com.proyecta.api_gestion.service.impl;

import com.proyecta.api_gestion.exception.InternalErrorException;
import com.proyecta.api_gestion.model.DocumentoInterno;
import com.proyecta.api_gestion.repository.DocumentoInternoRepository;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import com.proyecta.api_gestion.service.security.LocalUserAuthorizationService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentoInternoServiceImplTest {

    private DocumentoInternoRepository repository;
    private IStorageProvider storageProvider;
    private LocalUserAuthorizationService localUserAuthorizationService;
    private KeycloakIdentityExtractor identityExtractor;
    private DocumentoInternoServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(DocumentoInternoRepository.class);
        storageProvider = Mockito.mock(IStorageProvider.class);
        localUserAuthorizationService = Mockito.mock(LocalUserAuthorizationService.class);
        identityExtractor = Mockito.mock(KeycloakIdentityExtractor.class);
        service = new DocumentoInternoServiceImpl(
                repository, storageProvider, localUserAuthorizationService, identityExtractor);
    }

    @Test
    void formatCodigoUsaGranularidadPorMinuto() {
        LocalDateTime slot = LocalDateTime.of(2026, 9, 24, 10, 5);
        assertEquals("DOC-2026-09-24-10:05", DocumentoInternoServiceImpl.formatCodigo(slot));
    }

    @Test
    void formatCodigoMantieneCerosALaIzquierda() {
        LocalDateTime slot = LocalDateTime.of(2026, 1, 2, 7, 3);
        assertEquals("DOC-2026-01-02-07:03", DocumentoInternoServiceImpl.formatCodigo(slot));
    }

    @Test
    void asignarCodigoUsaSlotActualLibre() {
        when(repository.existsByCodigo(anyString())).thenReturn(false);
        String codigo = service.asignarCodigoUnico();
        assertTrue(codigo.matches("DOC-\\d{4}-\\d{2}-\\d{2}-\\d{2}:\\d{2}"));
    }

    @Test
    void asignarCodigoSaltaAlSiguienteMinutoSiOcupado() {
        String ocupado = DocumentoInternoServiceImpl.formatCodigo(LocalDateTime.now());
        when(repository.existsByCodigo(ocupado)).thenReturn(true);
        when(repository.existsByCodigo(argThat(c -> !c.equals(ocupado)))).thenReturn(false);

        String codigo = service.asignarCodigoUnico();
        assertNotEquals(ocupado, codigo);
        assertTrue(codigo.startsWith("DOC-"));
    }

    @Test
    void asignarCodigoFallaSiTodosLosSlotsEstanOcupados() {
        when(repository.existsByCodigo(anyString())).thenReturn(true);
        assertThrows(InternalErrorException.class, () -> service.asignarCodigoUnico());
    }

    @Test
    void buildLikePatternEscapaComodines() {
        assertEquals("%foo\\%bar\\_baz\\\\%", DocumentoInternoServiceImpl.buildLikePattern("Foo%Bar_Baz\\"));
    }

    @Test
    void buildLikePatternDevuelveNullSiVacio() {
        assertNull(DocumentoInternoServiceImpl.buildLikePattern(null));
        assertNull(DocumentoInternoServiceImpl.buildLikePattern("   "));
    }

    @Test
    void extraerExtensionIncluyePunto() {
        assertEquals(".pdf", DocumentoInternoServiceImpl.extraerExtension("documento.pdf"));
        assertEquals("", DocumentoInternoServiceImpl.extraerExtension("sin_extension"));
        assertEquals("", DocumentoInternoServiceImpl.extraerExtension(null));
    }

    @Test
    void guardarConCodigoUnicoReintegraTrasViolacionDeIntegridad() {
        when(repository.existsByCodigo(anyString())).thenReturn(false);
        when(repository.saveAndFlush(any(DocumentoInterno.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentoInterno resultado = service.guardarConCodigoUnico(
                "Nombre", "Desc", java.time.LocalDate.of(2026, 9, 24),
                "a.pdf", "doc_int_1.pdf", "application/pdf", 10L, "user");

        assertEquals("Nombre", resultado.getNombre());
        verify(repository, times(2)).saveAndFlush(any(DocumentoInterno.class));
    }
}
