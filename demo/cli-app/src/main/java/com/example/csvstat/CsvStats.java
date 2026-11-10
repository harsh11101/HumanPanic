package com.example.csvstat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Reads the "amount" column of a CSV file with a header row. */
final class CsvStats {

    List<Double> amounts(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException(file + " is empty");
        }
        int column = Arrays.asList(lines.get(0).split(",")).indexOf("amount");
        if (column < 0) {
            throw new IllegalArgumentException(file + " has no 'amount' column");
        }
        List<Double> amounts = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] cells = lines.get(i).split(",");
            try {
                amounts.add(Double.parseDouble(cells[column].trim()));
            } catch (NumberFormatException e) {
                throw new CsvFormatException(file, i + 1, "'" + cells[column] + "' is not a number", e);
            }
        }
        return amounts;
    }

    Stats summarize(Path file) {
        return Stats.of(amounts(file));
    }
}
