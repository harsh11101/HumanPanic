package io.pants.humanpanic.internal;

import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.IntConsumer;

/**
 * The single implementation of the {@link HumanPanic} handling policy. It is shared by the Spring
 * aspect, the JDK proxy and {@link io.pants.humanpanic.PanicHandler}. Not public API.
 *
 * <p>Invariant: handling a failure never throws an exception of its own. Any failure in reporting
 * or notification is logged through {@link System.Logger}, and the original failure keeps its
 * normal handling.
 */
public final class PanicResponder {

    private static final System.Logger LOG = System.getLogger(PanicResponder.class.getName());

    private final CrashReporter crashReporter;
    private final UserNotifier userNotifier;
    private final IntConsumer exit;

    /**
     * Creates a responder that terminates the JVM with {@link System#exit(int)}.
     *
     * @param crashReporter writes crash reports
     * @param userNotifier  writes user messages
     */
    public PanicResponder(CrashReporter crashReporter, UserNotifier userNotifier) {
        this(crashReporter, userNotifier, System::exit);
    }

    /**
     * Creates a responder with a custom exit action (for tests).
     *
     * @param crashReporter writes crash reports
     * @param userNotifier  writes user messages
     * @param exit          called with the exit code when {@link HumanPanic#exitCode()} is not zero
     */
    public PanicResponder(CrashReporter crashReporter, UserNotifier userNotifier, IntConsumer exit) {
        this.crashReporter = Objects.requireNonNull(crashReporter, "crashReporter");
        this.userNotifier = Objects.requireNonNull(userNotifier, "userNotifier");
        this.exit = Objects.requireNonNull(exit, "exit");
    }

    /**
     * Reports a failure (crash report, user message, optional stack trace) as the options say.
     * Never throws.
     *
     * @param failure the failure
     * @param options the options, or {@code null} for the annotation defaults
     * @param origin  the intercepted method, or {@code null}
     * @return the absolute report path, or {@code null} if none was written
     */
    public String report(Throwable failure, HumanPanic options, Method origin) {
        boolean createReport = options == null || options.createCrashReport();
        boolean silent = options != null && options.silent();
        boolean printStackTrace = options != null && options.printStackTrace();
        String message = options == null ? "" : options.message();

        String reportPath = null;
        try {
            if (createReport) {
                reportPath = crashReporter.createReport(failure, origin);
            }
            if (!silent) {
                if (createReport) {
                    userNotifier.notifyWithReport(message, reportPath);
                } else {
                    userNotifier.notify(message, failure);
                }
            }
            if (printStackTrace) {
                userNotifier.printStackTrace(failure);
            }
        } catch (RuntimeException | LinkageError e) {
            Diagnostics.warn(LOG, "HumanPanic: failed while handling " + failure, e);
        }
        return reportPath;
    }

    /**
     * Applies the full interception policy to a failure thrown by an annotated method:
     * <ol>
     *   <li>report it ({@link #report});</li>
     *   <li>restore the interrupt flag if it is an {@link InterruptedException};</li>
     *   <li>exit the JVM if {@code exitCode != 0};</li>
     *   <li>rethrow it if it is a {@link VirtualMachineError};</li>
     *   <li>otherwise return the default value for {@code returnType} (see
     *       {@link #defaultValue}).</li>
     * </ol>
     *
     * @param failure    the failure
     * @param options    the annotation, or {@code null} for defaults
     * @param origin     the intercepted method, or {@code null}
     * @param returnType the method's return type
     * @return the default value for {@code returnType}
     * @throws Throwable the original failure if it is a {@link VirtualMachineError}
     */
    public Object recover(Throwable failure, HumanPanic options, Method origin, Class<?> returnType)
            throws Throwable {
        report(failure, options, origin);
        if (failure instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        int exitCode = options == null ? 0 : options.exitCode();
        if (exitCode != 0) {
            exit.accept(exitCode);
        }
        if (failure instanceof VirtualMachineError) {
            throw failure;
        }
        return defaultValue(returnType);
    }

    /**
     * Returns the value an intercepted method returns after a handled failure:
     * <ul>
     *   <li>zero or {@code false} for primitives;</li>
     *   <li>an empty value for {@link Optional} (and its primitive variants);</li>
     *   <li>an empty immutable collection when the declared type is exactly {@link List},
     *       {@link Collection}, {@link Iterable}, {@link Set} or {@link Map};</li>
     *   <li>{@code null} for every other reference type and for {@code void}.</li>
     * </ul>
     *
     * @param type the declared return type; may be {@code null}
     * @return the default value
     */
    public static Object defaultValue(Class<?> type) {
        if (type == null || type == void.class) {
            return null;
        }
        if (!type.isPrimitive()) {
            return emptyValue(type);
        }
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        return '\u0000'; // char
    }

    private static Object emptyValue(Class<?> type) {
        if (type == Optional.class) return Optional.empty();
        if (type == OptionalInt.class) return OptionalInt.empty();
        if (type == OptionalLong.class) return OptionalLong.empty();
        if (type == OptionalDouble.class) return OptionalDouble.empty();
        if (type == List.class || type == Collection.class || type == Iterable.class) return List.of();
        if (type == Set.class) return Set.of();
        if (type == Map.class) return Map.of();
        return null;
    }
}
