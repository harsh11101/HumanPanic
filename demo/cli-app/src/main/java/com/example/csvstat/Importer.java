package com.example.csvstat;

import io.pants.humanpanic.HumanPanic;

import java.nio.file.Path;

/** Imports a file into a (pretend) database. A failure here must abort the whole process. */
public interface Importer {
    // Annotation on the interface method, with an exit code.
    @HumanPanic(exitCode = 2, message = "Import aborted; no data was changed.")
    int importFile(Path file);
}
