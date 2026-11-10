package com.example.salesmax;

import io.pants.humanpanic.PanicHandler;
import io.pants.humanpanic.config.AppMetadata;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * salesmax: HumanPanic in an Apache Commons CLI application.
 *
 * <pre>
 *   salesmax [-r &lt;dir&gt;] &lt;file.csv&gt;
 * </pre>
 *
 * This one handles the failure itself: a top-level try/catch calls {@code handle()} and then
 * picks its own exit status (70, "internal software error" in sysexits.h) instead of the JVM's 1.
 * The report directory is a command-line option.
 */
public final class SalesMax {

    private static final int EXIT_USAGE = 2;
    private static final int EXIT_SOFTWARE = 70;

    private SalesMax() {
    }

    public static void main(String[] args) {
        Options options = new Options()
                .addOption("r", "report-dir", true, "where crash reports are written (default: crash-reports)")
                .addOption("h", "help", false, "show this help");

        CommandLine cli;
        try {
            cli = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            // A usage error is the user's mistake, not a crash: no HumanPanic here.
            System.err.println(e.getMessage());
            usage(options);
            System.exit(EXIT_USAGE);
            return;
        }
        if (cli.hasOption("help") || cli.getArgList().size() != 1) {
            usage(options);
            System.exit(cli.hasOption("help") ? 0 : EXIT_USAGE);
        }

        AppMetadata app = new AppMetadata();
        app.setName("salesmax");
        app.setVersion("2.1.0");
        app.setAuthors(new String[]{"Grace Example <grace@example.com>"});
        PanicHandler panic = PanicHandler.builder()
                .metadata(app)
                .reportDirectory(Paths.get(cli.getOptionValue("report-dir", "crash-reports")))
                .build()
                .install();   // still covers other threads

        try {
            run(Paths.get(cli.getArgList().get(0)));
        } catch (Exception e) {
            panic.handle(e);              // friendly message + crash report; never throws
            System.exit(EXIT_SOFTWARE);   // our own exit status
        }
    }

    private static void run(Path file) throws IOException {
        if (!Files.isReadable(file)) {
            System.err.println("Cannot read file: " + file);
            System.exit(EXIT_USAGE);
        }
        List<String> lines = Files.readAllLines(file);
        int column = List.of(lines.get(0).split(",")).indexOf("amount");
        // A non-numeric cell is the "bug": Double.parseDouble throws NumberFormatException.
        double max = lines.subList(1, lines.size()).stream()
                .mapToDouble(line -> Double.parseDouble(line.split(",")[column].trim()))
                .max()
                .orElse(0);
        System.out.printf("max: %.2f%n", max);
    }

    private static void usage(Options options) {
        new HelpFormatter().printHelp("salesmax [options] <file.csv>", options);
    }
}
