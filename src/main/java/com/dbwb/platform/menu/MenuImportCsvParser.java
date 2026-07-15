package com.dbwb.platform.menu;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BR-IMP-001: parses the supported CSV template. Column order doesn't
 * matter - headers are matched by name (case-insensitive). Required
 * columns: Category, Name, Price. Optional: Description, Ingredients,
 * DiscountPrice, ImageUrl, MaxOrderQuantity.
 *
 * Only CSV is implemented here - true binary .xlsx parsing would need
 * Apache POI, a new dependency this change doesn't add unilaterally.
 * Spreadsheet software (Excel, Google Sheets, Numbers) all export to CSV,
 * so this still covers the "Excel/CSV" requirement in practice.
 */
public final class MenuImportCsvParser {

    private MenuImportCsvParser() {
    }

    public record RawRow(int rowNumber, Map<String, String> values) {
        public String get(String column) {
            String value = values.get(column.toLowerCase());
            return value == null || value.isBlank() ? null : value.trim();
        }
    }

    public static List<RawRow> parse(InputStream input) {
        List<RawRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BusinessRuleViolationException("The uploaded file is empty.");
            }
            List<String> headers = splitCsvLine(headerLine).stream().map(h -> h.trim().toLowerCase()).toList();

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> cells = splitCsvLine(line);
                Map<String, String> values = new LinkedHashMap<>();
                for (int i = 0; i < headers.size() && i < cells.size(); i++) {
                    values.put(headers.get(i), cells.get(i));
                }
                rows.add(new RawRow(rowNumber, values));
                rowNumber++;
            }
        } catch (IOException e) {
            throw new BusinessRuleViolationException("Could not read the uploaded file.");
        }
        return rows;
    }

    /** Minimal RFC4180-ish handling: quoted fields, escaped quotes, and commas inside quotes. */
    private static List<String> splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        result.add(current.toString());
        return result;
    }
}
