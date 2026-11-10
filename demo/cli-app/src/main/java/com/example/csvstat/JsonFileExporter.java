package com.example.csvstat;

import io.pants.humanpanic.HumanPanic;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Exports the summary as JSON. An export failure is reported but must not stop the tool. */
final class JsonFileExporter implements StatsExporter {

    // Annotation on the implementation method. The proxy finds it here (fixed in 2.0.0).
    // createCrashReport=false: a short message is enough for this non-fatal failure.
    @HumanPanic(createCrashReport = false,
            message = "Could not export the summary; the statistics above are still valid.")
    @Override
    public boolean export(Stats stats, Path target) throws Exception {
        String json = String.format(Locale.ROOT,
                "{\"count\":%d,\"sum\":%.2f,\"mean\":%.2f}%n", stats.count(), stats.sum(), stats.mean());
        Files.writeString(target, json, StandardCharsets.UTF_8); // throws if target dir is missing
        return true;
    }
}
