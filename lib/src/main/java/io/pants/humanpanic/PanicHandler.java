package io.pants.humanpanic;

import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.interceptor.HumanPanicProxy;
import io.pants.humanpanic.internal.PanicResponder;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

/**
 * The framework-free entry point to HumanPanic.
 *
 * <pre>{@code
 * public static void main(String[] args) {
 *     AppMetadata app = new AppMetadata();
 *     app.setName("csvtool");
 *     app.setVersion("1.4.0");
 *     app.setIssueUrl("https://github.com/acme/csvtool/issues");
 *
 *     PanicHandler.builder().metadata(app).build().install();
 *     // ... any uncaught exception now produces a friendly message and a crash report
 * }
 * }</pre>
 *
 * <p>Three ways to use a handler:
 * <ul>
 *   <li>{@link #install()}: becomes the JVM's default uncaught-exception handler. This is the
 *       equivalent of Rust's {@code setup_panic!()}. The JVM's own behaviour is unchanged: the
 *       thread still dies, and an uncaught exception in {@code main} still ends the process with
 *       exit status 1.</li>
 *   <li>{@link #wrap(Object, Class)}: a proxy that applies {@link HumanPanic} handling to the
 *       annotated methods of an interface implementation.</li>
 *   <li>{@link #handle(Throwable)}: report a failure you caught yourself.</li>
 * </ul>
 *
 * <p>Instances are immutable and thread-safe.
 */
public final class PanicHandler {

    private final CrashReporter crashReporter;
    private final UserNotifier userNotifier;
    private final PanicResponder responder;

    private PanicHandler(Builder builder) {
        AppMetadata metadata = new AppMetadata(builder.metadata);
        this.crashReporter = new CrashReporter(metadata, builder.reportDirectory);
        this.userNotifier = builder.output == null
                ? new UserNotifier(metadata)
                : new UserNotifier(metadata, builder.output);
        this.responder = new PanicResponder(crashReporter, userNotifier);
    }

    /**
     * Starts building a handler. The defaults are default {@link AppMetadata}, reports in
     * {@code ./crash-reports}, and messages on {@link System#err}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Reports a failure: writes a crash report and shows the user message that points to it.
     * Never throws for problems while reporting.
     *
     * @param failure the failure; must not be {@code null}
     * @return the crash report location, or empty if it could not be written
     */
    public Optional<Path> handle(Throwable failure) {
        Objects.requireNonNull(failure, "failure");
        return Optional.ofNullable(responder.report(failure, null, null)).map(Paths::get);
    }

    /**
     * Returns an uncaught-exception handler that calls {@link #handle(Throwable)}, for use with
     * {@link Thread#setUncaughtExceptionHandler} or a {@code ThreadFactory}.
     *
     * @return the handler
     */
    public Thread.UncaughtExceptionHandler asUncaughtExceptionHandler() {
        return (thread, failure) -> handle(failure);
    }

    /**
     * Installs this handler as the JVM-wide default uncaught-exception handler (see
     * {@link Thread#setDefaultUncaughtExceptionHandler}). Threads and thread groups with their own
     * handler are not affected.
     *
     * @return this handler
     */
    public PanicHandler install() {
        Thread.setDefaultUncaughtExceptionHandler(asUncaughtExceptionHandler());
        return this;
    }

    /**
     * Wraps an implementation in a proxy that applies {@link HumanPanic} handling to its annotated
     * methods (the annotation may be on the implementation or the interface method).
     *
     * @param target         the implementation
     * @param interfaceClass the interface to expose
     * @param <T>            the interface type
     * @return the proxy
     * @throws IllegalArgumentException if {@code target} does not implement {@code interfaceClass}
     */
    public <T> T wrap(T target, Class<T> interfaceClass) {
        return HumanPanicProxy.wrap(target, interfaceClass, crashReporter, userNotifier);
    }

    /**
     * Returns where this handler writes crash reports.
     *
     * @return the report directory
     */
    public Path getReportDirectory() {
        return crashReporter.getReportDirectory();
    }

    /** Builder for {@link PanicHandler}. Not thread-safe. */
    public static final class Builder {
        private AppMetadata metadata = new AppMetadata();
        private Path reportDirectory = Paths.get(CrashReporter.DEFAULT_REPORT_DIRECTORY);
        private PrintStream output;

        private Builder() {
        }

        /**
         * Sets the application metadata. It is copied when {@link #build()} is called, so later
         * changes to {@code metadata} have no effect on the built handler.
         *
         * @param metadata the metadata; must not be {@code null}
         * @return this builder
         */
        public Builder metadata(AppMetadata metadata) {
            this.metadata = Objects.requireNonNull(metadata, "metadata");
            return this;
        }

        /**
         * Sets the crash report directory. It is created when the first report is written.
         *
         * @param reportDirectory the directory; must not be {@code null}
         * @return this builder
         */
        public Builder reportDirectory(Path reportDirectory) {
            this.reportDirectory = Objects.requireNonNull(reportDirectory, "reportDirectory");
            return this;
        }

        /**
         * Sets the stream for user messages. If this is never called, {@link System#err} is used.
         *
         * @param output the stream; must not be {@code null}
         * @return this builder
         */
        public Builder output(PrintStream output) {
            this.output = Objects.requireNonNull(output, "output");
            return this;
        }

        /**
         * Builds the handler.
         *
         * @return a new, immutable handler
         */
        public PanicHandler build() {
            return new PanicHandler(this);
        }
    }
}
