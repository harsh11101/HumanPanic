package io.pants.humanpanic.reporter;

import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.ConfigLoader;

import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Writes the human-friendly panic message, in the spirit of Rust's {@code human-panic}, to a
 * {@link PrintStream}. The default stream is {@link System#err}, looked up each time a message is
 * written, so {@link System#setErr(PrintStream)} is respected.
 *
 * <p>Each message is written with a single {@code print} call and then flushed, so messages from
 * concurrent failures do not interleave. Instances are thread-safe.
 */
public class UserNotifier {

    private final Supplier<AppMetadata> metadata;
    private final Supplier<PrintStream> out;

    /**
     * Creates a notifier that reads metadata from a {@link ConfigLoader} (Spring integration) and
     * writes to {@link System#err}.
     *
     * @param configLoader the metadata source; must not be {@code null}
     */
    public UserNotifier(ConfigLoader configLoader) {
        Objects.requireNonNull(configLoader, "configLoader");
        this.metadata = configLoader::getMetadata;
        this.out = () -> System.err;
    }

    /**
     * Creates a notifier with explicit metadata that writes to {@link System#err}.
     *
     * @param metadata the application metadata; must not be {@code null}
     */
    public UserNotifier(AppMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata");
        this.metadata = () -> metadata;
        this.out = () -> System.err;
    }

    /**
     * Creates a notifier with explicit metadata and output stream.
     *
     * @param metadata the application metadata; must not be {@code null}
     * @param out      where messages are written; must not be {@code null}
     */
    public UserNotifier(AppMetadata metadata, PrintStream out) {
        Objects.requireNonNull(metadata, "metadata");
        Objects.requireNonNull(out, "out");
        this.metadata = () -> metadata;
        this.out = () -> out;
    }

    /**
     * Shows the full panic message, pointing the user to the crash report.
     *
     * @param customMessage an optional extra sentence; {@code null} or blank for none
     * @param reportPath    the report location, or {@code null} if no report could be written
     */
    public void notifyWithReport(String customMessage, String reportPath) {
        AppMetadata md = metadata.get();
        String name = appName(md);

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("Well, this is embarrassing.\n");
        sb.append("\n");
        sb.append(name).append(" had a problem and crashed. To help us diagnose\n");
        sb.append("the problem you can send us a crash report.\n");
        sb.append("\n");

        if (hasText(customMessage)) {
            sb.append(customMessage).append("\n");
            sb.append("\n");
        }

        if (reportPath != null) {
            sb.append("We have generated a report file at:\n");
            sb.append("\n");
            sb.append("  ").append(reportPath).append("\n");
            sb.append("\n");
        } else {
            sb.append("Unfortunately, the crash report could not be written.\n");
            sb.append("\n");
        }

        sb.append("Submit an issue or email with the subject of:\n");
        sb.append("\n");
        sb.append("  ").append(name).append(" Crash Report\n");
        sb.append("\n");

        String issueUrl = md == null ? null : md.getIssueUrl();
        if (hasText(issueUrl)) {
            sb.append("- Open an issue at:\n");
            sb.append("    ").append(issueUrl).append("\n");
            sb.append("\n");
        }

        String supportUrl = md == null ? null : md.getSupportUrl();
        if (hasText(supportUrl)) {
            sb.append("- Get help at:\n");
            sb.append("    ").append(supportUrl).append("\n");
            sb.append("\n");
        }

        String[] authors = md == null ? null : md.getAuthors();
        if (authors != null && authors.length > 0) {
            sb.append("- Contact the authors:\n");
            for (String author : authors) {
                if (hasText(author)) {
                    sb.append("    ").append(author).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("We take privacy seriously, and do not perform any automated error collection.\n");
        sb.append("In order to improve the software, we rely on people to submit reports.\n");
        sb.append("\n");
        sb.append("Thank you kindly!\n");

        emit(sb);
    }

    /**
     * Shows a short error message without a crash report reference.
     *
     * @param message   an optional extra sentence; {@code null} or blank for none
     * @param throwable the failure whose type and message are shown; may be {@code null}
     */
    public void notify(String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("Well, this is embarrassing.\n");
        sb.append("\n");
        sb.append(appName(metadata.get())).append(" encountered an error:\n");

        if (hasText(message)) {
            sb.append("  ").append(message).append("\n");
        }
        if (throwable != null) {
            sb.append("  ").append(throwable.getClass().getSimpleName()).append("\n");
            if (throwable.getMessage() != null) {
                sb.append("  ").append(throwable.getMessage()).append("\n");
            }
        }
        sb.append("\n");

        emit(sb);
    }

    /**
     * Prints the raw stack trace of a failure to this notifier's output stream.
     *
     * @param throwable the failure; ignored if {@code null}
     */
    public void printStackTrace(Throwable throwable) {
        if (throwable != null) {
            PrintStream stream = out.get();
            throwable.printStackTrace(stream);
            stream.flush();
        }
    }

    private void emit(CharSequence text) {
        PrintStream stream = out.get();
        stream.print(text);
        stream.flush();
    }

    private static String appName(AppMetadata md) {
        String name = md == null ? null : md.getName();
        return hasText(name) ? name : AppMetadata.DEFAULT_NAME;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
