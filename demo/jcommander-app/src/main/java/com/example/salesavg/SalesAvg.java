package com.example.salesavg;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import io.pants.humanpanic.PanicHandler;
import io.pants.humanpanic.config.AppMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * salesavg: HumanPanic in a JCommander application.
 *
 * <pre>
 *   salesavg [--column amount] &lt;file.csv&gt;
 * </pre>
 *
 * JCommander only parses arguments; it does not run your code or catch its exceptions. So
 * {@code install()} is all that is needed: anything uncaught reaches HumanPanic, and the JVM ends
 * with exit status 1 as usual.
 */
public final class SalesAvg {

    @Parameter(description = "<file.csv>", required = true)
    private String file;

    @Parameter(names = {"-c", "--column"}, description = "Column to average")
    private String column = "amount";

    @Parameter(names = {"-h", "--help"}, help = true, description = "Show this help")
    private boolean help;

    public static void main(String[] args) throws IOException {
        AppMetadata app = new AppMetadata();
        app.setName("salesavg");
        app.setVersion("0.1.0");
        app.setSupportUrl("https://example.com/salesavg/help");
        PanicHandler.builder().metadata(app).build().install();

        SalesAvg command = new SalesAvg();
        JCommander cli = JCommander.newBuilder().programName("salesavg").addObject(command).build();
        try {
            cli.parse(args);
        } catch (ParameterException e) {
            // A usage error is the user's mistake, not a crash: no HumanPanic here.
            System.err.println(e.getMessage());
            cli.usage();
            System.exit(2);
        }
        if (command.help) {
            cli.usage();
            return;
        }
        command.run();   // no try/catch on purpose: a bug crashes into the global handler
    }

    private void run() throws IOException {
        Path path = Paths.get(file);
        if (!Files.isReadable(path)) {
            System.err.println("Cannot read file: " + path);
            System.exit(2);
        }
        List<String> lines = Files.readAllLines(path);
        int index = List.of(lines.get(0).split(",")).indexOf(column);
        if (index < 0) {
            System.err.println("No such column: " + column);
            System.exit(2);
        }
        // A non-numeric cell is the "bug": Double.parseDouble throws NumberFormatException.
        double average = lines.subList(1, lines.size()).stream()
                .mapToDouble(line -> Double.parseDouble(line.split(",")[index].trim()))
                .average()
                .orElse(0);
        System.out.printf("average %s: %.2f%n", column, average);
    }
}
