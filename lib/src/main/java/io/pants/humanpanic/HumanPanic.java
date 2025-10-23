package io.pants.humanpanic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable human-friendly panic handling for methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface HumanPanic {
    String message() default "";
    boolean printStackTrace() default false;
    boolean createCrashReport() default true;
    int exitCode() default 0;
    boolean silent() default false;
}
