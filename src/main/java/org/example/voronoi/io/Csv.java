package org.example.voronoi.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal CSV reader: UTF-8, optional header, blank lines and {@code #} comments
 * skipped. Values must not contain commas, which holds for numeric station and
 * signalling data.
 */
final class Csv {

    private Csv() {
    }

    static List<Row> read(Path path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + path, e);
        }

        List<Row> rows = new ArrayList<>();
        boolean headerSkipped = false;
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (i == 0 && raw.startsWith("\uFEFF")) {
                raw = raw.substring(1);
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split(",", -1);
            for (int f = 0; f < fields.length; f++) {
                fields[f] = fields[f].trim();
            }
            if (!headerSkipped) {
                headerSkipped = true;
                if (!isNumeric(fields[0])) {
                    continue;
                }
            }
            rows.add(new Row(path, i + 1, fields));
        }
        return rows;
    }

    private static boolean isNumeric(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** One parsed line with typed accessors that report the source location. */
    static final class Row {

        private final Path path;
        private final int line;
        private final String[] fields;

        Row(Path path, int line, String[] fields) {
            this.path = path;
            this.line = line;
            this.fields = fields;
        }

        int size() {
            return fields.length;
        }

        String text(int index) {
            require(index);
            return fields[index];
        }

        int integer(int index) {
            require(index);
            try {
                return Integer.parseInt(fields[index]);
            } catch (NumberFormatException e) {
                throw error(index, "an integer");
            }
        }

        long longValue(int index) {
            require(index);
            try {
                return Long.parseLong(fields[index]);
            } catch (NumberFormatException e) {
                throw error(index, "a long");
            }
        }

        double decimal(int index) {
            require(index);
            try {
                return Double.parseDouble(fields[index]);
            } catch (NumberFormatException e) {
                throw error(index, "a decimal");
            }
        }

        private void require(int index) {
            if (index >= fields.length) {
                throw error(index, "a value");
            }
        }

        private IllegalArgumentException error(int index, String expected) {
            return new IllegalArgumentException(
                    "Invalid CSV at " + path + ":" + line + ", column " + (index + 1) + " expects " + expected);
        }
    }
}
