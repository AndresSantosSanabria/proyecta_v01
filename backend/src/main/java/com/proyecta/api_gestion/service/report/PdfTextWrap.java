package com.proyecta.api_gestion.service.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class PdfTextWrap {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private PdfTextWrap() {
    }

    @FunctionalInterface
    interface WidthFn {
        float widthOf(String text) throws IOException;
    }

    static List<String> wrapLines(String text, float maxWidth, WidthFn measure) throws IOException {
        String source = text == null ? "" : text;
        String[] rawLines = source.split("\\R", -1);
        List<String> result = new ArrayList<>();
        for (String rawLine : rawLines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                result.add("");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (String word : WHITESPACE.split(line)) {
                appendWord(word, current, result, maxWidth, measure);
            }
            if (!current.isEmpty()) {
                result.add(current.toString());
            }
        }
        return result.isEmpty() ? List.of("") : result;
    }

    static void appendWordTrial(String word, StringBuilder current, List<String> result, float maxWidth, WidthFn measure) throws IOException {
        String trial = current.isEmpty() ? word : current + " " + word;
        if (measure.widthOf(trial) <= maxWidth) {
            current.setLength(0);
            current.append(trial);
            return;
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
            current.setLength(0);
        }
        if (measure.widthOf(word) <= maxWidth) {
            current.append(word);
        } else {
            result.addAll(splitLongWord(word, maxWidth, measure));
        }
    }

    private static void appendWord(String word, StringBuilder current, List<String> result, float maxWidth, WidthFn measure) throws IOException {
        String trial = current.isEmpty() ? word : current + " " + word;
        if (measure.widthOf(trial) <= maxWidth) {
            current.setLength(0);
            current.append(trial);
            return;
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        current.setLength(0);
        current.append(word);
    }

    private static List<String> splitLongWord(String word, float maxWidth, WidthFn measure) throws IOException {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (char c : word.toCharArray()) {
            String trial = current + String.valueOf(c);
            if (measure.widthOf(trial) <= maxWidth || current.isEmpty()) {
                current.append(c);
            } else {
                parts.add(current.toString());
                current = new StringBuilder().append(c);
            }
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }
        return parts;
    }
}
