package com.example.salesctl;

import io.pants.humanpanic.PanicHandler;
import io.pants.humanpanic.config.AppMetadata;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * salesctl: HumanPanic in a picocli application.
 *
 * <pre>
 *   salesctl total &lt;file.csv&gt;
 *   salesctl top   &lt;file.csv&gt; [-n 3]
 * </pre>
 *
 * The one thing to know: picocli catches every exception a command throws, so the JVM's
 * uncaught-exception handler never sees it. Hand the failure to HumanPanic in picocli's
 * execution-exception handler instead (see {@link #main}).
 */
@Command(name = "salesctl", version = SalesCtl.VERSION, mixinStandardHelpOptions = true,
        description = "Sums and ranks the 'amount' column of a CSV file.",
        subcommands = {SalesCtl.Total.class, SalesCtl.Top.class})
public final class SalesCtl {

    static final String VERSION = "0.3.0";

    public static void main(String[] args) {
        CommandLine cli = new CommandLine(new SalesCtl());

        AppMetadata app = new AppMetadata();
        app.setName(cli.getCommandName());          // one source of truth: the @Command annotation
        app.setVersion(VERSION);
        app.setIssueUrl("https://github.com/example/salesctl/issues");

        // install() still matters: it covers threads that picocli does not run.
        PanicHandler panic = PanicHandler.builder().metadata(app).build().install();

        // Unexpected failure inside a command: friendly message + crash report, picocli's exit code.
        cli.setExecutionExceptionHandler((failure, commandLine, parseResult) -> {
            panic.handle(failure);
            return commandLine.getCommandSpec().exitCodeOnExecutionException();   // 1
        });

        // Usage errors (unknown option, unreadable file) stay with picocli: usage text, exit 2.
        System.exit(cli.execute(args));
    }

    @Command(name = "total", description = "Prints the sum of all amounts.")
    static final class Total implements Callable<Integer> {
        @Spec CommandSpec spec;
        @Parameters(paramLabel = "<file.csv>") Path file;

        @Override
        public Integer call() throws IOException {
            double sum = amounts(spec, file).stream().mapToDouble(Double::doubleValue).sum();
            System.out.printf("total: %.2f%n", sum);
            return 0;
        }
    }

    @Command(name = "top", description = "Prints the largest amounts.")
    static final class Top implements Callable<Integer> {
        @Spec CommandSpec spec;
        @Parameters(paramLabel = "<file.csv>") Path file;
        @Option(names = "-n", defaultValue = "3", description = "How many (default: ${DEFAULT-VALUE}).") int n;

        @Override
        public Integer call() throws IOException {
            List<Double> amounts = amounts(spec, file);
            amounts.sort(Comparator.reverseOrder());
            amounts.subList(0, Math.min(n, amounts.size())).forEach(a -> System.out.printf("%.2f%n", a));
            return 0;
        }
    }

    /** Reads the "amount" column. A non-numeric cell is the "bug": it throws NumberFormatException. */
    private static List<Double> amounts(CommandSpec spec, Path file) throws IOException {
        if (!Files.isReadable(file)) {
            // An expected mistake by the user, not a crash: let picocli report it.
            throw new ParameterException(spec.commandLine(), "Cannot read file: " + file);
        }
        List<String> lines = Files.readAllLines(file);
        int column = List.of(lines.get(0).split(",")).indexOf("amount");
        List<Double> amounts = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            amounts.add(Double.parseDouble(line.split(",")[column].trim()));
        }
        return amounts;
    }
}
