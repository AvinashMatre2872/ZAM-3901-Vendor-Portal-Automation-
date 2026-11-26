package com.zamplia.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small CSV reader (handles quotes, commas and new lines inside quotes, Excel BOM). */
public final class CsvUtils {

    private static final Pattern ISO_DATE = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");

    private CsvUtils() { }

    /** Returns all non-empty rows. Row 0 is the header. */
    public static List<List<String>> read(File file) throws IOException {
        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) text = text.substring(1);

        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                row.add(field.toString().trim());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(field.toString().trim());
                field.setLength(0);
                addIfNotEmpty(rows, row);
                row = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        row.add(field.toString().trim());
        addIfNotEmpty(rows, row);
        return rows;
    }

    private static void addIfNotEmpty(List<List<String>> rows, List<String> row) {
        for (String v : row) {
            if (!v.isEmpty()) {
                rows.add(row);
                return;
            }
        }
    }

    /** Index of the first header containing any keyword (case-insensitive), -1 if none. */
    public static int columnIndex(List<String> header, String... keywords) {
        for (String k : keywords) {
            for (int i = 0; i < header.size(); i++) {
                if (header.get(i).toLowerCase().contains(k.toLowerCase())) return i;
            }
        }
        return -1;
    }

    /** Distinct non-empty values of one column (data rows only). */
    public static Set<String> distinctValues(List<List<String>> rows, int col) {
        Set<String> out = new LinkedHashSet<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            if (col < r.size() && !r.get(col).isEmpty()) out.add(r.get(col));
        }
        return out;
    }

    /** Extracts a yyyy-MM-dd date from a cell like "2026-10-05 13:22:10", or null. */
    public static LocalDate extractDate(String value) {
        Matcher m = ISO_DATE.matcher(value == null ? "" : value);
        return m.find() ? LocalDate.parse(m.group(1)) : null;
    }
}
