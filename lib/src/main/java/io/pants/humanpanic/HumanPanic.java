package io.pants.humanpanic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method whose unexpected failures should be handled "humanely": the user sees a short,
 * friendly message instead of a stack trace, and a JSON crash report is written for diagnosis.
 *
 * <p>The annotation does nothing by itself. It takes effect when the method is called through one of
 * the supported integrations:
 * <ul>
 *   <li>a Spring bean, with {@code io.pants.humanpanic.config.HumanPanicConfiguration} imported
 *       (Spring AOP);</li>
 *   <li>a JDK proxy created with {@link PanicHandler#wrap(Object, Class)} or
 *       {@code io.pants.humanpanic.interceptor.HumanPanicProxy}.</li>
 * </ul>
 * For failures that nothing else catches (for example a crash in {@code main}), use
 * {@link PanicHandler#install()} instead.
 *
 * <p><b>The exception is swallowed.</b> When an intercepted method throws, the exception is handled
 * as described by the attributes below and the caller receives the method's default return value
 * ({@code null}, {@code 0}, {@code false}, ...), unless {@link #exitCode()} terminates the JVM. Two
 * exceptions to this rule:
 * <ul>
 *   <li>a {@link VirtualMachineError} (such as {@link OutOfMemoryError} or
 *       {@link StackOverflowError}) is reported and then rethrown, because the JVM cannot safely
 *       continue;</li>
 *   <li>an {@link InterruptedException} is reported, and the thread's interrupt flag is restored.</li>
 * </ul>
 *
 * <p>Only use this annotation at boundaries where "report the failure and carry on with a default
 * value" (or "report and exit") is genuinely what you want, such as a CLI command's entry method.
 *
 * <p>Constructors are allowed as a target only for source compatibility with 1.x. No supported
 * integration intercepts constructors, so the annotation has no effect there.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface HumanPanic {

    /**
     * An extra, human-readable sentence shown to the user beneath the standard panic text. An empty
     * or blank string means no extra message.
     *
     * @return the custom message, or {@code ""} for none
     */
    String message() default "";

    /**
     * Also print the raw stack trace to the user-facing output stream. Useful during development.
     *
     * @return whether to print the stack trace
     */
    boolean printStackTrace() default false;

    /**
     * Write a JSON crash report file. When this is {@code false}, the user message names the exception
     * type and message instead of a report location.
     *
     * @return whether to write a crash report
     */
    boolean createCrashReport() default true;

    /**
     * When not zero, the JVM is terminated with {@code System.exit(exitCode)} after the failure has been
     * reported. Zero (the default) means the method returns its default value and the application
     * keeps running.
     *
     * @return the process exit code, or {@code 0} to keep running
     */
    int exitCode() default 0;

    /**
     * Suppress the user-facing message. A crash report is still written if
     * {@link #createCrashReport()} is {@code true}.
     *
     * @return whether to suppress the user message
     */
    boolean silent() default false;
}
