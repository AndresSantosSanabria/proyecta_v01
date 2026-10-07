package com.proyecta.api_gestion.service.report;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

record FontPack(PDFont regular, PDFont bold, boolean unicode) {

    private static final Logger log = LoggerFactory.getLogger(FontPack.class);

    static FontPack load(PDDocument document) {
        List<String[]> candidates = List.of(
                new String[]{"C:\\Windows\\Fonts\\calibri.ttf", "C:\\Windows\\Fonts\\calibrib.ttf"},
                new String[]{"C:\\Windows\\Fonts\\arial.ttf", "C:\\Windows\\Fonts\\arialbd.ttf"}
        );
        return load(document, candidates);
    }

    static FontPack load(PDDocument document, List<String[]> candidates) {
        for (String[] candidate : candidates) {
            try {
                File regular = new File(candidate[0]);
                File bold = new File(candidate[1]);
                if (regular.isFile() && bold.isFile()) {
                    return new FontPack(
                            PDType0Font.load(document, regular),
                            PDType0Font.load(document, bold),
                            true
                    );
                }
            } catch (IOException ex) {
                log.debug("No se pudieron cargar las fuentes personalizadas del reporte: {}", ex.toString());
            }
        }

        return new FontPack(
                new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                false
        );
    }
}
