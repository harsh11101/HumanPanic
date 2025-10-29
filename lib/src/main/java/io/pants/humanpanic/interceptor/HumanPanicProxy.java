package io.pants.humanpanic.interceptor;

import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Dynamic proxy implementation for @HumanPanic annotation.
 */
@RequiredArgsConstructor
public class HumanPanicProxy implements InvocationHandler {

    private Object target;
    private final CrashReporter crashReporter;
    private final UserNotifier userNotifier;

    /**
     * Private constructor for creating instances
     */
    private HumanPanicProxy(Object target, CrashReporter crashReporter, UserNotifier userNotifier) {
        this.target = target;
        this.crashReporter = crashReporter;
        this.userNotifier = userNotifier;
    }

    /**
     * Creates a new proxy instance with dependencies
     */
    public <T> T createProxy(T target) {
        this.target = target;
        return wrap(target, crashReporter, userNotifier);
    }

    /**
     * Wraps an object with HumanPanic proxy.
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(T target, CrashReporter crashReporter, UserNotifier userNotifier) {
        if (target == null) {
            throw new IllegalArgumentException("Target object cannot be null");
        }

        Class<?>[] interfaces = target.getClass().getInterfaces();
        if (interfaces.length == 0) {
            throw new IllegalArgumentException(
                    "Target must implement at least one interface. " +
                            "Use wrap(target, interfaceClass) or AspectJ for concrete classes."
            );
        }

        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                interfaces,
                new HumanPanicProxy(target, crashReporter, userNotifier)
        );
    }

    /**
     * Wraps an object with HumanPanic proxy for a specific interface.
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(Object target, Class<T> interfaceClass,
                             CrashReporter crashReporter, UserNotifier userNotifier) {
        if (target == null) {
            throw new IllegalArgumentException("Target object cannot be null");
        }
        if (interfaceClass == null || !interfaceClass.isInterface()) {
            throw new IllegalArgumentException("Must provide a valid interface class");
        }
        if (!interfaceClass.isAssignableFrom(target.getClass())) {
            throw new IllegalArgumentException(
                    "Target must implement " + interfaceClass.getName()
            );
        }

        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                new Class<?>[]{interfaceClass},
                new HumanPanicProxy(target, crashReporter, userNotifier)
        );
    }

    /**
     * Checks if an object is already wrapped by HumanPanicProxy
     */
    public static boolean isWrapped(Object obj) {
        return Proxy.isProxyClass(obj.getClass())
                && Proxy.getInvocationHandler(obj) instanceof HumanPanicProxy;
    }

    /**
     * Unwraps a proxied object to get the original target
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(Object proxy) {
        if (!isWrapped(proxy)) {
            throw new IllegalArgumentException("Object is not a HumanPanic proxy");
        }

        HumanPanicProxy handler = (HumanPanicProxy) Proxy.getInvocationHandler(proxy);
        return (T) handler.target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        HumanPanic annotation = method.getAnnotation(HumanPanic.class);

        if (annotation == null) {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            handleException(cause, annotation, method);

            if (annotation.exitCode() != 0) {
                System.exit(annotation.exitCode());
            }

            return getDefaultReturnValue(method.getReturnType());
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            handleException(cause, annotation, method);

            if (annotation.exitCode() != 0) {
                System.exit(annotation.exitCode());
            }

            return getDefaultReturnValue(method.getReturnType());
        }
    }

    private void handleException(Throwable throwable, HumanPanic annotation, Method method) {
        if (annotation.createCrashReport()) {
            String reportPath = crashReporter.createReport(throwable, method);
            if (!annotation.silent()) {
                userNotifier.notifyWithReport(
                        annotation.message().isEmpty() ? "An error occurred" : annotation.message(),
                        reportPath
                );
            }
        } else if (!annotation.silent()) {
            userNotifier.notify(
                    annotation.message().isEmpty() ? "An error occurred" : annotation.message(),
                    throwable
            );
        }

        if (annotation.printStackTrace()) {
            throwable.printStackTrace();
        }
    }

    private Object getDefaultReturnValue(Class<?> returnType) {
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }

        if (returnType.isPrimitive()) {
            if (returnType == boolean.class) return false;
            if (returnType == byte.class) return (byte) 0;
            if (returnType == short.class) return (short) 0;
            if (returnType == int.class) return 0;
            if (returnType == long.class) return 0L;
            if (returnType == float.class) return 0.0f;
            if (returnType == double.class) return 0.0d;
            if (returnType == char.class) return '\u0000';
        }

        return null;
    }
}