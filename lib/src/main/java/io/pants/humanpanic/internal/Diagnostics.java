package io.pants.humanpanic.internal;

/**
 * Logs HumanPanic's own problems (for example, a report that cannot be written) through
 * {@link System.Logger}. By default that is {@code java.util.logging}, unless the application
 * bridges it to another framework. Not public API.
 *
 * <p>The WARNING record is a single line, because it may reach the same terminal the user is
 * reading, and a stack trace there defeats the purpose of the library. The stack trace is logged
 * separately at DEBUG level.
 */
public final class Diagnostics {

    private Diagnostics() {
    }

    /**
     * Logs a one-line warning, plus the full stack trace at DEBUG level.
     *
     * @param log     the logger
     * @param message what HumanPanic was trying to do
     * @param error   what went wrong
     */
    public static void warn(System.Logger log, String message, Throwable error) {
        log.log(System.Logger.Level.WARNING, message + ": " + error);
        log.log(System.Logger.Level.DEBUG, message, error);
    }
}
