package com.example.csvstat;

import java.nio.file.Path;

/** A CSV file contains a value that cannot be parsed. */
final class CsvFormatException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    CsvFormatException(Path file, int line, String problem, Throwable cause) {
        super(file + ", line " + line + ": " + problem, cause);
    }
}
