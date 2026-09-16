package com.proyecta.api_gestion.service;

import com.proyecta.api_gestion.config.PublicUrlProperties;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.model.*;
import com.proyecta.api_gestion.model.enums.EstrategiaPeti;
import com.proyecta.api_gestion.repository.*;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioProyectoRepository;
import com.proyecta.api_gestion.service.impl.ProjectProgressMetricsService;
import com.proyecta.api_gestion.service.impl.ReporteServiceImpl;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import com.proyecta.api_gestion.service.security.dynamic.ProyectoSecurity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ReporteServiceImplExcelTest {

    private ProyectoRepository proyectoRepository;
    private EntregableRepository entregableRepository;
    private RiesgoRepository riesgoRepository;
    private ReporteConfigRepository reporteConfigRepository;
    private ProjectProgressMetricsService progressMetricsService;
    private SeguridadUsuarioProyectoRepository seguridadUsuarioProyectoRepository;
    private KeycloakIdentityExtractor identityExtractor;
    private ProyectoSecurity proyectoSecurity;
    private PublicEvidenceAccessService publicEvidenceAccessService;
    private PublicUrlProperties publicUrlProperties;

    private ReporteServiceImpl reporteService;

    @BeforeEach
    void setUp() {
        proyectoRepository = Mockito.mock(ProyectoRepository.class);
        entregableRepository = Mockito.mock(EntregableRepository.class);
        riesgoRepository = Mockito.mock(RiesgoRepository.class);
        reporteConfigRepository = Mockito.mock(ReporteConfigRepository.class);
        progressMetricsService = Mockito.mock(ProjectProgressMetricsService.class);
        seguridadUsuarioProyectoRepository = Mockito.mock(SeguridadUsuarioProyectoRepository.class);
        identityExtractor = Mockito.mock(KeycloakIdentityExtractor.class);
        proyectoSecurity = Mockito.mock(ProyectoSecurity.class);
        publicEvidenceAccessService = Mockito.mock(PublicEvidenceAccessService.class);
        publicUrlProperties = Mockito.mock(PublicUrlProperties.class);

        when(publicUrlProperties.getBase()).thenReturn("http://localhost:8080");

        reporteService = new ReporteServiceImpl(
                proyectoRepository, entregableRepository, riesgoRepository,
                reporteConfigRepository, progressMetricsService, seguridadUsuarioProyectoRepository,
                identityExtractor, proyectoSecurity, publicEvidenceAccessService, publicUrlProperties
        );
    }

    @Test
    void testGenerarReporteActualProyectoExcel() throws IOException {
        Proyecto p = new Proyecto();
        try {
            var field = Proyecto.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(p, "P02");
        } catch (Exception e) {
            fail(e);
        }
        p.setNombre("Actualización Cundilex");
        p.setAlcanceDetallado("Alcance test");
        p.setDependencia("SECRETARÍA JURÍDICA");
        p.setDirector("GERMÁN GÓMEZ");
        p.setCorreoDirector("german@cundinamarca.gov.co");
        p.setObjetivoGeneral("Objetivo general");
        p.setFechaInicio(LocalDate.of(2025, 1, 1));
        p.setPresupuestoEstimado(new BigDecimal("100000000"));
        p.setPeti(true);
        p.setEstrategiaPeti(EstrategiaPeti.TRANSFORMACION_DIGITAL);

        Patrocinador pat = new Patrocinador();
        pat.setNombre("Patrocinador Test");
        p.setPatrocinador(pat);

        Fase fase1 = new Fase();
        fase1.setId(1);
        fase1.setNombre("FASE 1");
        fase1.setPonderacion(new BigDecimal("50"));
        fase1.setProyecto(p);

        Hito hito1 = new Hito();
        hito1.setId(1);
        hito1.setNombre("HITO 1");
        hito1.setPonderacion(new BigDecimal("50"));
        hito1.setFase(fase1);

        Entregable e1 = new Entregable();
        e1.setId(1);
        e1.setNombre("E01. Documento Requerimientos");
        e1.setDescripcion("E01. Documento de Requerimientos");
        e1.setPonderacion(new BigDecimal("50"));
        e1.setFechaLimite(LocalDate.of(2025, 10, 15));
        e1.setHito(hito1);

        Entregable e2 = new Entregable();
        e2.setId(2);
        e2.setNombre("E02. Diseño Arquitectura");
        e2.setDescripcion("E02. Diseño de Arquitectura");
        e2.setPonderacion(new BigDecimal("50"));
        e2.setFechaLimite(LocalDate.of(2025, 11, 15));
        e2.setHito(hito1);

        hito1.getEntregables().add(e1);
        hito1.getEntregables().add(e2);
        fase1.getHitos().add(hito1);
        p.getFases().add(fase1);

        when(proyectoRepository.findById("P02")).thenReturn(Optional.of(p));

        ProyectoAvanceResponseDTO avanceMock = Mockito.mock(ProyectoAvanceResponseDTO.class);
        when(avanceMock.progresoProgramado()).thenReturn(new BigDecimal("50.0"));
        when(avanceMock.progresoEjecutado()).thenReturn(new BigDecimal("50.0"));
        when(avanceMock.estado()).thenReturn("EN_TIEMPO");

        when(progressMetricsService.construir(eq(p), any(LocalDate.class))).thenReturn(avanceMock);

        byte[] excelBytes = reporteService.generarReporteActualProyectoExcel("P02");
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            assertEquals(3, wb.getNumberOfSheets());

            Sheet s1 = wb.getSheetAt(0);
            assertEquals("DATOS", s1.getSheetName());
            assertEquals("P02", s1.getRow(1).getCell(2).getStringCellValue());
            assertEquals("SI", s1.getRow(16).getCell(2).getStringCellValue());

            Sheet s2 = wb.getSheetAt(1);
            assertEquals("AVANCE", s2.getSheetName());
            assertEquals("FASE 1", s2.getRow(4).getCell(1).getStringCellValue());
            assertEquals("(G5*L5)+(G6*L6)", s2.getRow(4).getCell(14).getCellFormula());

            Sheet s3 = wb.getSheetAt(2);
            assertEquals("INFORME 1", s3.getSheetName());
            assertEquals("AVANCE!W5", s3.getRow(5).getCell(2).getCellFormula());
            assertEquals("AVANCE!V5", s3.getRow(7).getCell(2).getCellFormula());
            assertEquals("AVANCE!U5", s3.getRow(9).getCell(2).getCellFormula());
        }
    }
}
