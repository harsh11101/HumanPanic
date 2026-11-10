package com.example.csvstat;

import java.nio.file.Path;

/** Writes a summary somewhere. Implementations may annotate methods with @HumanPanic. */
public interface StatsExporter {
    boolean export(Stats stats, Path target) throws Exception;
}
