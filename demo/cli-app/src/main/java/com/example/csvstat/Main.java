package com.example.csvstat;

import io.pants.humanpanic.PanicHandler;
import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.NonSpringConfigLoader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * csvstat: prints statistics for the "amount" column of CSV files.
 *
 * <pre>
 *   csvstat stats  &lt;file.csv&gt;               summary; a bad file crashes into HumanPanic's global handler
 *   csvstat export &lt;file.csv&gt; &lt;out.json&gt;  summary + export through a @HumanPanic proxy (failure is non-fatal)
 *   csvstat batch  &lt;file.csv&gt;...            many files; failures handled manually, processing continues
 *   csvstat import &lt;file.csv&gt;               @HumanPanic(exitCode = 2): a failure aborts with exit status 2
 * </pre>
 *
 * Set {@code -Dcsvstat.reportDir=...} to change where crash reports go.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        // 1. Application metadata from application.properties (classpath) + -Dapp.* overrides.
        AppMetadata app = new NonSpringConfigLoader().load();

        // 2. One handler for the whole process. install() covers every uncaught exception.
        PanicHandler panic = PanicHandler.builder()
                .metadata(app)
                .reportDirectory(Paths.get(System.getProperty("csvstat.reportDir", "crash-reports")))
                .build()
                .install();

        if (args.length < 2) {
            System.out.println("usage: csvstat (stats|export|batch|import) <file.csv> [...]");
            System.exit(64);
        }

        CsvStats csv = new CsvStats();
        switch (args[0]) {
            case "stats":
                // No try/catch on purpose: a corrupt or missing file is an unexpected crash.
                System.out.println(csv.summarize(Paths.get(args[1])));
                break;

            case "export": {
                Stats stats = csv.summarize(Paths.get(args[1]));
                System.out.println(stats);
                StatsExporter exporter = panic.wrap(new JsonFileExporter(), StatsExporter.class);
                boolean exported = exporter.export(stats, Paths.get(args[2]));
                System.out.println(exported ? "Exported to " + args[2] : "Export skipped; continuing.");
                break;
            }

            case "batch": {
                List<String> files = Arrays.asList(args).subList(1, args.length);
                int ok = 0;
                for (String file : files) {
                    try {
                        System.out.println(file + ": " + csv.summarize(Paths.get(file)));
                        ok++;
                    } catch (Exception e) {
                        // Manual use: report this failure, keep processing the rest.
                        Optional<Path> report = panic.handle(e);
                        System.out.println(file + ": FAILED (report: "
                                + report.map(Path::toString).orElse("not written") + ")");
                    }
                }
                System.out.println(ok + "/" + files.size() + " files processed");
                break;
            }

            case "import": {
                Importer importer = panic.wrap(new DatabaseImporter(csv), Importer.class);
                int rows = importer.importFile(Paths.get(args[1]));
                System.out.println("Imported " + rows + " rows");
                break;
            }

            default:
                System.out.println("unknown command: " + args[0]);
                System.exit(64);
        }
    }
}
