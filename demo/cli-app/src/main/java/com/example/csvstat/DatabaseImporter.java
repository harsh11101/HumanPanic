package com.example.csvstat;

import java.nio.file.Path;

final class DatabaseImporter implements Importer {
    private final CsvStats csv;

    DatabaseImporter(CsvStats csv) {
        this.csv = csv;
    }

    @Override
    public int importFile(Path file) {
        try {
            return csv.amounts(file).size();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Import of " + file + " failed", e);
        }
    }
}
