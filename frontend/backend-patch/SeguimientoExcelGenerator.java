package com.proyecta.api_gestion.service.report;

import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.model.Fase;
import com.proyecta.api_gestion.model.Hito;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class SeguimientoExcelGenerator {

    private final ProyectoRepository proyectoRepository;

    public SeguimientoExcelGenerator(ProyectoRepository proyectoRepository) {
        this.proyectoRepository = proyectoRepository;
    }

    public byte[] generate(String projectId) throws IOException {
        Proyecto proyecto = proyectoRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + projectId));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            buildCronogramaSheet(workbook, proyecto);
            workbook.setForceFormulaRecalculation(true); // Forces Excel to evaluate formulas when opening

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void buildCronogramaSheet(XSSFWorkbook workbook, Proyecto proyecto) {
        XSSFSheet sheet = workbook.createSheet("AVANCE");

        // Palette styles
        XSSFCellStyle headerStyle = createHeaderStyle(workbook, new XSSFColor(new java.awt.Color(31, 78, 121), new DefaultIndexedColorMap())); // Dark Blue (#1F4E79)
        XSSFCellStyle subHeaderStyle = createHeaderStyle(workbook, new XSSFColor(new java.awt.Color(47, 117, 181), new DefaultIndexedColorMap())); // Lighter Blue (#2F75B8)
        XSSFCellStyle greenHeaderStyle = createHeaderStyle(workbook, new XSSFColor(new java.awt.Color(55, 86, 35), new DefaultIndexedColorMap())); // Dark Green (#375623)

        XSSFCellStyle phase1Style = createPhaseStyle(workbook, new XSSFColor(new java.awt.Color(255, 242, 204), new DefaultIndexedColorMap())); // Light Yellow
        XSSFCellStyle phase2Style = createPhaseStyle(workbook, new XSSFColor(new java.awt.Color(189, 215, 238), new DefaultIndexedColorMap())); // Light Blue

        XSSFCellStyle[] hitoStyles = new XSSFCellStyle[]{
                createHitoStyle(workbook, new XSSFColor(new java.awt.Color(255, 217, 102), new DefaultIndexedColorMap())), // Yellow
                createHitoStyle(workbook, new XSSFColor(new java.awt.Color(155, 194, 230), new DefaultIndexedColorMap())), // Blue
                createHitoStyle(workbook, new XSSFColor(new java.awt.Color(224, 191, 229), new DefaultIndexedColorMap())), // Purple
                createHitoStyle(workbook, new XSSFColor(new java.awt.Color(169, 208, 142), new DefaultIndexedColorMap()))  // Green
        };

        XSSFCellStyle executedHitoStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(255, 217, 102), new DefaultIndexedColorMap()), "0.0%"); // Yellow
        XSSFCellStyle executedFaseStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(255, 217, 102), new DefaultIndexedColorMap()), "0.0%"); // Yellow
        XSSFCellStyle executedPytoStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(169, 208, 142), new DefaultIndexedColorMap()), "0.0%"); // Green

        XSSFCellStyle progHitoStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(255, 217, 102), new DefaultIndexedColorMap()), "0.0%"); // Yellow
        XSSFCellStyle progFaseStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(255, 217, 102), new DefaultIndexedColorMap()), "0.0%"); // Yellow
        XSSFCellStyle progPytoStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(169, 208, 142), new DefaultIndexedColorMap()), "0.0%"); // Green

        XSSFCellStyle eyeStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(255, 217, 102), new DefaultIndexedColorMap()), null); // Yellow
        XSSFCellStyle eficaciaCalcStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(169, 208, 142), new DefaultIndexedColorMap()), null); // Green
        XSSFCellStyle eficienciaCalcStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(189, 215, 238), new DefaultIndexedColorMap()), null); // Light Blue

        // Ponderacion percent styles (same background colors as phase/hito but with percent format)
        XSSFCellStyle phase1PercentStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(255, 242, 204), new DefaultIndexedColorMap()), "0%"); // Light Yellow
        XSSFCellStyle phase2PercentStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(189, 215, 238), new DefaultIndexedColorMap()), "0%"); // Light Blue
        XSSFCellStyle[] hitoPercentStyles = new XSSFCellStyle[]{
                createColoredStyle(workbook, new XSSFColor(new java.awt.Color(255, 217, 102), new DefaultIndexedColorMap()), "0%"), // Yellow
                createColoredStyle(workbook, new XSSFColor(new java.awt.Color(155, 194, 230), new DefaultIndexedColorMap()), "0%"), // Blue
                createColoredStyle(workbook, new XSSFColor(new java.awt.Color(224, 191, 229), new DefaultIndexedColorMap()), "0%"), // Purple
                createColoredStyle(workbook, new XSSFColor(new java.awt.Color(169, 208, 142), new DefaultIndexedColorMap()), "0%")  // Green
        };

        XSSFCellStyle greenSummaryStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(55, 86, 35), new DefaultIndexedColorMap()), "0.0%");
        XSSFFont fontWhite = workbook.createFont();
        fontWhite.setBold(true);
        fontWhite.setColor(IndexedColors.WHITE.getIndex());
        greenSummaryStyle.setFont(fontWhite);

        XSSFDataFormat df = workbook.createDataFormat();

        XSSFCellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(df.getFormat("d-mmm-yy"));
        setBorder(dateStyle);
        dateStyle.setAlignment(HorizontalAlignment.CENTER);

        XSSFCellStyle percentStyle = workbook.createCellStyle();
        percentStyle.setDataFormat(df.getFormat("0.0%"));
        setBorder(percentStyle);
        percentStyle.setAlignment(HorizontalAlignment.CENTER);

        XSSFCellStyle percentTwoDecStyle = workbook.createCellStyle();
        percentTwoDecStyle.setDataFormat(df.getFormat("0.00%"));
        setBorder(percentTwoDecStyle);
        percentTwoDecStyle.setAlignment(HorizontalAlignment.CENTER);

        XSSFCellStyle defaultStyle = workbook.createCellStyle();
        setBorder(defaultStyle);
        defaultStyle.setAlignment(HorizontalAlignment.CENTER);

        XSSFCellStyle textStyle = workbook.createCellStyle();
        setBorder(textStyle);
        textStyle.setAlignment(HorizontalAlignment.LEFT);

        // Header Rows (0 to 3)
        // Row 0: Title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue(proyecto.getId() + " - " + (proyecto.getNombre() != null ? proyecto.getNombre() : ""));
        titleCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 25));

        Row row1 = sheet.createRow(1); // Spacing row

        Row row2 = sheet.createRow(2); // Subheader 1
        Row row3 = sheet.createRow(3); // Subheader 2

        String[] subHeader1 = new String[26];
        subHeader1[1] = "FASE"; subHeader1[2] = "FASE";
        subHeader1[3] = "HITO"; subHeader1[4] = "HITO";
        for (int i = 5; i <= 10; i++) subHeader1[i] = "Entregable";
        subHeader1[11] = "ENTREGA"; subHeader1[12] = "Entregable"; subHeader1[13] = "COMENTARIOS";
        subHeader1[14] = "EJECUTADO"; subHeader1[15] = "EJECUTADO"; subHeader1[16] = "EJECUTADO";
        subHeader1[17] = "ATRASO"; subHeader1[18] = "ATRASO"; subHeader1[19] = "ATRASO";
        subHeader1[20] = "PROG HOY"; subHeader1[21] = "PROG HOY"; subHeader1[22] = "PROG HOY";
        subHeader1[23] = "EyE"; subHeader1[24] = "Eficacia"; subHeader1[25] = "Eficiencia";

        String[] subHeader2 = new String[26];
        subHeader2[1] = "FASES"; subHeader2[2] = "Pond";
        subHeader2[3] = "HITOS"; subHeader2[4] = "Ponderado";
        subHeader2[5] = "ENTREGABLES"; subHeader2[6] = "Ponderado"; subHeader2[7] = "NOMBRE";
        subHeader2[8] = "FECHA LIM"; subHeader2[9] = "FECHA ENTREGA"; subHeader2[10] = "ATRASO";
        subHeader2[11] = "OK"; subHeader2[12] = "RUTA EVIDENCIAS"; subHeader2[13] = "EVIDENCIAS";
        subHeader2[14] = "HITO"; subHeader2[15] = "FASE"; subHeader2[16] = "PYTO";
        subHeader2[17] = "HOY"; subHeader2[18] = "PROG"; subHeader2[19] = "VALIDA";
        subHeader2[20] = "HITO PROG"; subHeader2[21] = "FASE PROG"; subHeader2[22] = "PYTO PROG";
        subHeader2[23] = "Incluye?"; subHeader2[24] = "Calcula"; subHeader2[25] = "Calcula";

        for (int i = 1; i <= 25; i++) {
            Cell c1 = row2.createCell(i);
            c1.setCellValue(subHeader1[i] != null ? subHeader1[i] : "");
            c1.setCellStyle(i == 25 ? greenHeaderStyle : headerStyle);

            Cell c2 = row3.createCell(i);
            c2.setCellValue(subHeader2[i] != null ? subHeader2[i] : "");
            c2.setCellStyle(i == 25 ? greenHeaderStyle : subHeaderStyle);
        }

        // Merging for row 2 (Subheader 1)
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 2)); // FASE
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 3, 4)); // HITO
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 5, 10)); // Entregable
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 14, 16)); // EJECUTADO
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 17, 19)); // ATRASO
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 20, 22)); // PROG HOY

        int rowIndex = 4; // Data starts at row 5 (0-indexed 4)
        int phaseCount = 0;

        List<Fase> fases = proyecto.getFases();
        for (Fase fase : fases) {
            int phaseStartRow = rowIndex;
            XSSFCellStyle currentPhaseStyle = (phaseCount % 2 == 0) ? phase1Style : phase2Style;

            List<Hito> hitos = fase.getHitos();
            int hitoIndex = 0;

            for (Hito hito : hitos) {
                int hitoStartRow = rowIndex;
                XSSFCellStyle currentHitoStyle = hitoStyles[hitoIndex % hitoStyles.length];

                List<Entregable> entregables = hito.getEntregables();
                for (int e = 0; e < entregables.size(); e++) {
                    Entregable entregable = entregables.get(e);
                    Row row = sheet.createRow(rowIndex);
                    int r = rowIndex + 1; // 1-based index for formulas

                    // 1 (B): Fase Nombre
                    Cell c1 = row.createCell(1);
                    c1.setCellValue(fase.getNombre());
                    c1.setCellStyle(currentPhaseStyle);

                    // 2 (C): Fase Ponderacion - use percent style matching phase background
                    Cell c2 = row.createCell(2);
                    c2.setCellValue(fase.getPonderacion().doubleValue() / 100.0);
                    c2.setCellStyle(phaseCount % 2 == 0 ? phase1PercentStyle : phase2PercentStyle);

                    // 3 (D): Hito Nombre
                    Cell c3 = row.createCell(3);
                    c3.setCellValue(hito.getNombre());
                    c3.setCellStyle(currentHitoStyle);

                    // 4 (E): Hito Ponderacion - use percent style matching hito background
                    Cell c4 = row.createCell(4);
                    c4.setCellValue(hito.getPonderacion().doubleValue() / 100.0);
                    c4.setCellStyle(hitoPercentStyles[hitoIndex % hitoPercentStyles.length]);

                    // 5 (F): Entregable Codigo
                    Cell c5 = row.createCell(5);
                    c5.setCellValue("E" + String.format("%02d", e + 1));
                    c5.setCellStyle(defaultStyle);

                    // 6 (G): Entregable Ponderacion
                    Cell c6 = row.createCell(6);
                    c6.setCellValue(entregable.getPonderacion().doubleValue() / 100.0);
                    c6.setCellStyle(percentStyle);

                    // 7 (H): Entregable Nombre
                    Cell c7 = row.createCell(7);
                    c7.setCellValue(entregable.getNombre());
                    c7.setCellStyle(textStyle);

                    // 8 (I): Fecha Limite
                    Cell c8 = row.createCell(8);
                    if (entregable.getFechaLimite() != null) {
                        c8.setCellValue(java.sql.Date.valueOf(entregable.getFechaLimite()));
                        c8.setCellStyle(dateStyle);
                    } else {
                        c8.setCellStyle(defaultStyle);
                    }

                    // 9 (J): Fecha Entrega
                    Cell c9 = row.createCell(9);
                    if (entregable.getFechaEntregaEfectiva() != null) {
                        c9.setCellValue(java.sql.Date.valueOf(entregable.getFechaEntregaEfectiva()));
                        c9.setCellStyle(dateStyle);
                    } else {
                        c9.setCellStyle(defaultStyle);
                    }

                    // 10 (K): ATRASO
                    Cell c10 = row.createCell(10);
                    c10.setCellFormula("IF(I"+r+"=\"\",\"\",IF(J"+r+"=\"\",MIN(0,I"+r+"-R"+r+"),I"+r+"-J"+r+"))");
                    c10.setCellStyle(defaultStyle);

                    // 11 (L): OK
                    Cell c11 = row.createCell(11);
                    c11.setCellFormula("IF(ISNUMBER(J"+r+"),1,0)");
                    c11.setCellStyle(defaultStyle);

                    // 12 (M): RUTA EVIDENCIAS
                    Cell c12 = row.createCell(12);
                    if (entregable.getArchivoPdf() != null) {
                        c12.setCellValue("Evidencia");
                        CreationHelper createHelper = workbook.getCreationHelper();
                        Hyperlink link = createHelper.createHyperlink(HyperlinkType.URL);
                        link.setAddress(entregable.getArchivoPdf());
                        c12.setHyperlink(link);

                        XSSFCellStyle linkStyle = workbook.createCellStyle();
                        XSSFFont linkFont = workbook.createFont();
                        linkFont.setUnderline(FontUnderline.SINGLE);
                        linkFont.setColor(IndexedColors.BLUE.getIndex());
                        linkStyle.setFont(linkFont);
                        setBorder(linkStyle);
                        c12.setCellStyle(linkStyle);
                    } else {
                        c12.setCellStyle(defaultStyle);
                    }

                    // 13 (N): Empty/Hidden
                    Cell c13 = row.createCell(13);
                    c13.setCellStyle(defaultStyle);

                    // 14 (O): EJECUTADO HITO
                    Cell c14 = row.createCell(14);
                    c14.setCellStyle(executedHitoStyle);

                    // 15 (P): EJECUTADO FASE
                    Cell c15 = row.createCell(15);
                    c15.setCellStyle(executedFaseStyle);

                    // 16 (Q): EJECUTADO PYTO
                    Cell c16 = row.createCell(16);
                    c16.setCellStyle(executedPytoStyle);

                    // 17 (R): TODAY() - MUST use English name in OOXML format.
                    // Apache POI stores formulas in English internally.
                    // Spanish Excel reads TODAY() from XML and displays/evaluates it correctly.
                    // Using HOY() would store an unknown function → #¿NOMBRE? error.
                    Cell c17 = row.createCell(17);
                    c17.setCellFormula("TODAY()");
                    c17.setCellStyle(dateStyle);

                    // 18 (S): ATRASO PROG
                    Cell c18 = row.createCell(18);
                    c18.setCellFormula("I"+r+"-R"+r);
                    c18.setCellStyle(defaultStyle);

                    // 19 (T): VALIDA
                    Cell c19 = row.createCell(19);
                    c19.setCellFormula("IF(S"+r+">0,0,1)");
                    c19.setCellStyle(defaultStyle);

                    // 20 (U): PROG HITO
                    Cell c20 = row.createCell(20);
                    c20.setCellStyle(progHitoStyle);

                    // 21 (V): PROG FASE
                    Cell c21 = row.createCell(21);
                    c21.setCellStyle(progFaseStyle);

                    // 22 (W): PROG PYTO
                    Cell c22 = row.createCell(22);
                    c22.setCellStyle(progPytoStyle);

                    // 23 (X): EyE Incluye?
                    Cell c23 = row.createCell(23);
                    c23.setCellFormula("IF(I"+r+"<=R"+r+",\"Si\",\"No\")");
                    c23.setCellStyle(eyeStyle);

                    // 24 (Y): Eficacia Calcula
                    Cell c24 = row.createCell(24);
                    c24.setCellFormula("IF(AND(I"+r+"<=R"+r+",J"+r+"<>\"\",L"+r+"=1),\"Si\",\"\")");
                    c24.setCellStyle(eficaciaCalcStyle);

                    // 25 (Z): Eficiencia Calcula
                    Cell c25 = row.createCell(25);
                    c25.setCellFormula("IF(AND(I"+r+"<>\"\",J"+r+"<>\"\",L"+r+"=1,J"+r+"<=I"+r+"),\"Si\",\"\")");
                    c25.setCellStyle(eficienciaCalcStyle);

                    rowIndex++;
                }

                // Merge Hito columns vertically
                if (rowIndex - 1 > hitoStartRow) {
                    sheet.addMergedRegion(new CellRangeAddress(hitoStartRow, rowIndex - 1, 3, 3));
                    sheet.addMergedRegion(new CellRangeAddress(hitoStartRow, rowIndex - 1, 4, 4));
                    sheet.addMergedRegion(new CellRangeAddress(hitoStartRow, rowIndex - 1, 14, 14)); // Ejecutado Hito
                    sheet.addMergedRegion(new CellRangeAddress(hitoStartRow, rowIndex - 1, 20, 20)); // Prog Hito
                }
                hitoIndex++;
            }

            // Merge Fase columns vertically
            if (rowIndex - 1 > phaseStartRow) {
                sheet.addMergedRegion(new CellRangeAddress(phaseStartRow, rowIndex - 1, 1, 1));
                sheet.addMergedRegion(new CellRangeAddress(phaseStartRow, rowIndex - 1, 2, 2));
                sheet.addMergedRegion(new CellRangeAddress(phaseStartRow, rowIndex - 1, 15, 15)); // Ejecutado Fase
                sheet.addMergedRegion(new CellRangeAddress(phaseStartRow, rowIndex - 1, 21, 21)); // Prog Fase
            }
            phaseCount++;
        }

        int finalRow = rowIndex > 4 ? rowIndex : 5; // 1-based index of last data row

        // Update range-dependent formulas with exact finalRow
        for (int i = 4; i < rowIndex; i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                int r = i + 1;
                Cell c14 = row.getCell(14);
                if (c14 != null) c14.setCellFormula("SUMPRODUCT(($D$5:$D$" + finalRow + "=D" + r + ")*$G$5:$G$" + finalRow + "*$L$5:$L$" + finalRow + ")");

                Cell c15 = row.getCell(15);
                if (c15 != null) c15.setCellFormula("SUMPRODUCT(($B$5:$B$" + finalRow + "=B" + r + ")*$E$5:$E$" + finalRow + "*$O$5:$O$" + finalRow + ")");

                Cell c16 = row.getCell(16);
                if (c16 != null) c16.setCellFormula("AVERAGE($P$5:$P$" + finalRow + ")");

                Cell c20 = row.getCell(20);
                if (c20 != null) c20.setCellFormula("IFERROR(AVERAGEIFS($T$5:$T$" + finalRow + ",$D$5:$D$" + finalRow + ",D" + r + "),0)");

                Cell c21 = row.getCell(21);
                if (c21 != null) c21.setCellFormula("IFERROR(AVERAGEIFS($U$5:$U$" + finalRow + ",$B$5:$B$" + finalRow + ",B" + r + "),0)");

                Cell c22 = row.getCell(22);
                if (c22 != null) c22.setCellFormula("AVERAGE($V$5:$V$" + finalRow + ")");
            }
        }

        // Merge Pyto columns vertically across all rows
        if (rowIndex > 5) {
            sheet.addMergedRegion(new CellRangeAddress(4, rowIndex - 1, 16, 16)); // Ejecutado Pyto
            sheet.addMergedRegion(new CellRangeAddress(4, rowIndex - 1, 22, 22)); // Prog Pyto
        }

        // Summary row right under table (Row finalRow + 1)
        Row summaryRow = sheet.createRow(rowIndex);
        Cell summaryY = summaryRow.createCell(24); // Col Y (24)
        summaryY.setCellFormula("COUNTIFS($Y$5:$Y$" + finalRow + ",\"Si\",$X$5:$X$" + finalRow + ",\"Si\")/COUNTIF($X$5:$X$" + finalRow + ",\"Si\")");
        summaryY.setCellStyle(greenSummaryStyle);

        Cell summaryZ = summaryRow.createCell(25); // Col Z (25)
        summaryZ.setCellFormula("COUNTIFS($Z$5:$Z$" + finalRow + ",\"Si\",$Y$5:$Y$" + finalRow + ",\"Si\")/COUNTIF($Y$5:$Y$" + finalRow + ",\"Si\")");
        summaryZ.setCellStyle(greenSummaryStyle);

        // INDICADORES AL CORTE summary box (Rows finalRow + 3 to finalRow + 9)
        int indStartRow = rowIndex + 2; // 0-indexed row for "INDICADORES AL CORTE"

        Row rIndHeader = sheet.createRow(indStartRow);
        for (int c = 23; c <= 25; c++) {
            Cell cell = rIndHeader.createCell(c);
            cell.setCellStyle(greenHeaderStyle);
        }
        Cell indHeaderCell = rIndHeader.getCell(23);
        indHeaderCell.setCellValue("INDICADORES AL CORTE");
        sheet.addMergedRegion(new CellRangeAddress(indStartRow, indStartRow, 23, 25));

        XSSFCellStyle lightGreenStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(226, 239, 218), new DefaultIndexedColorMap()), null);
        XSSFCellStyle lightGreenPercentStyle = createColoredStyle(workbook, new XSSFColor(new java.awt.Color(226, 239, 218), new DefaultIndexedColorMap()), "0.00%");
        XSSFFont fontBold = workbook.createFont();
        fontBold.setBold(true);
        lightGreenPercentStyle.setFont(fontBold);

        int corteRow1Based = 5; // Reference to R5 (HOY)

        // Row 1: Fecha de corte
        Row r1 = sheet.createRow(indStartRow + 1);
        Cell r1x = r1.createCell(23); r1x.setCellValue("Fecha de corte"); r1x.setCellStyle(textStyle);
        Cell r1y = r1.createCell(24); r1y.setCellFormula("R" + corteRow1Based); r1y.setCellStyle(dateStyle);
        Cell r1z = r1.createCell(25); r1z.setCellValue("Base del cálculo"); r1z.setCellStyle(textStyle);

        int fechaCorteRow1Based = indStartRow + 2; // 1-based index of "Fecha de corte" row

        // Row 2: Programados al corte
        Row r2 = sheet.createRow(indStartRow + 2);
        Cell r2x = r2.createCell(23); r2x.setCellValue("Programados al corte"); r2x.setCellStyle(textStyle);
        Cell r2y = r2.createCell(24); r2y.setCellFormula("COUNTIFS(I5:I" + finalRow + ",\"<=\"&Y" + fechaCorteRow1Based + ",I5:I" + finalRow + ",\"<>\")"); r2y.setCellStyle(defaultStyle);
        Cell r2z = r2.createCell(25); r2z.setCellValue("Entregables con fecha límite menor o igual a la fecha de corte"); r2z.setCellStyle(textStyle);

        // Row 3: Entregados al corte
        Row r3 = sheet.createRow(indStartRow + 3);
        Cell r3x = r3.createCell(23); r3x.setCellValue("Entregados al corte"); r3x.setCellStyle(textStyle);
        Cell r3y = r3.createCell(24); r3y.setCellFormula("COUNTIFS(J5:J" + finalRow + ",\"<=\"&Y" + fechaCorteRow1Based + ",J5:J" + finalRow + ",\"<>\",L5:L" + finalRow + ",1)"); r3y.setCellStyle(defaultStyle);
        Cell r3z = r3.createCell(25); r3z.setCellValue("Programados al corte con OK = 1"); r3z.setCellStyle(textStyle);

        int progCorteRow1Based = indStartRow + 3;
        int entCorteRow1Based = indStartRow + 4;

        // Row 4: Entregados a tiempo
        Row r4 = sheet.createRow(indStartRow + 4);
        Cell r4x = r4.createCell(23); r4x.setCellValue("Entregados a tiempo"); r4x.setCellStyle(textStyle);
        Cell r4y = r4.createCell(24); r4y.setCellFormula("SUMPRODUCT(--(J5:J" + finalRow + "<=Y" + fechaCorteRow1Based + "),--(I5:I" + finalRow + "<>\"\"),--(L5:L" + finalRow + "=1),--(J5:J" + finalRow + "<>\"\"),--(J5:J" + finalRow + "<=I5:I" + finalRow + "))"); r4y.setCellStyle(defaultStyle);
        Cell r4z = r4.createCell(25); r4z.setCellValue("Entregados al corte con fecha de entrega menor o igual a la fecha límite"); r4z.setCellStyle(textStyle);

        int entTiempoRow1Based = indStartRow + 5;

        // Row 5: Eficacia
        Row r5 = sheet.createRow(indStartRow + 5);
        Cell r5x = r5.createCell(23); r5x.setCellValue("Eficacia"); r5x.setCellStyle(lightGreenStyle);
        Cell r5y = r5.createCell(24); r5y.setCellFormula("MIN(1,IFERROR(Y" + entCorteRow1Based + "/Y" + progCorteRow1Based + ",0))"); r5y.setCellStyle(lightGreenPercentStyle);
        Cell r5z = r5.createCell(25); r5z.setCellValue("Entregados al corte / Programados al corte"); r5z.setCellStyle(lightGreenStyle);

        // Row 6: Eficiencia
        Row r6 = sheet.createRow(indStartRow + 6);
        Cell r6x = r6.createCell(23); r6x.setCellValue("Eficiencia"); r6x.setCellStyle(lightGreenStyle);
        Cell r6y = r6.createCell(24); r6y.setCellFormula("MIN(1,IFERROR(Y" + entTiempoRow1Based + "/Y" + entCorteRow1Based + ",0))"); r6y.setCellStyle(lightGreenPercentStyle);
        Cell r6z = r6.createCell(25); r6z.setCellValue("Entregados a tiempo / Entregados al corte"); r6z.setCellStyle(lightGreenStyle);

        // Hide columns 0 and 13
        sheet.setColumnHidden(0, true);
        sheet.setColumnHidden(13, true);

        // Set explicit column widths (in units of 1/256th of a character width)
        sheet.setColumnWidth(1,  4500);  // B: FASES
        sheet.setColumnWidth(2,  2500);  // C: POND
        sheet.setColumnWidth(3,  4500);  // D: HITOS
        sheet.setColumnWidth(4,  2800);  // E: Ponderado
        sheet.setColumnWidth(5,  3000);  // F: ENTREGABLES
        sheet.setColumnWidth(6,  2800);  // G: Ponderado
        sheet.setColumnWidth(7,  10000); // H: NOMBRE (wide for description)
        sheet.setColumnWidth(8,  3800);  // I: FECHA LIM
        sheet.setColumnWidth(9,  4000);  // J: FECHA ENTREGA
        sheet.setColumnWidth(10, 3000);  // K: ATRASO
        sheet.setColumnWidth(11, 2200);  // L: OK
        sheet.setColumnWidth(12, 5000);  // M: RUTA EVIDENCIAS
        sheet.setColumnWidth(14, 3500);  // O: EJ HITO
        sheet.setColumnWidth(15, 3500);  // P: EJ FASE
        sheet.setColumnWidth(16, 3500);  // Q: EJ PYTO
        sheet.setColumnWidth(17, 3800);  // R: HOY
        sheet.setColumnWidth(18, 3000);  // S: ATRASO PROG
        sheet.setColumnWidth(19, 2800);  // T: VALIDA
        sheet.setColumnWidth(20, 4000);  // U: HITO PROG
        sheet.setColumnWidth(21, 4000);  // V: FASE PROG
        sheet.setColumnWidth(22, 4000);  // W: PYTO PROG
        sheet.setColumnWidth(23, 3500);  // X: EyE Incluye?
        sheet.setColumnWidth(24, 4500);  // Y: Eficacia Calcula
        sheet.setColumnWidth(25, 4500);  // Z: Eficiencia Calcula

        // Set default row height
        sheet.setDefaultRowHeightInPoints(30);
    }

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook workbook, XSSFColor color) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    private XSSFCellStyle createPhaseStyle(XSSFWorkbook workbook, XSSFColor color) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    private XSSFCellStyle createHitoStyle(XSSFWorkbook workbook, XSSFColor color) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    private XSSFCellStyle createColoredStyle(XSSFWorkbook workbook, XSSFColor color, String dataFormat) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);
        if (dataFormat != null) {
            XSSFDataFormat df = workbook.createDataFormat();
            style.setDataFormat(df.getFormat(dataFormat));
        }
        setBorder(style);
        return style;
    }

    private void setBorder(XSSFCellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}

