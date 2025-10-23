package io.pants.humanpanic.interceptor;

import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Dynamic proxy implementation for @HumanPanic annotation.
 * This provides a fallback mechanism when AspectJ is not available
 * or when you want to manually control which objects have panic handling.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * // Wrap an interface implementation
 * MyService service = new MyServiceImpl();
 * MyService wrappedService = HumanPanicProxy.wrap(service, MyService.class);
 *
 * // Or use the convenience method for single interface
 * MyService wrappedService = HumanPanicProxy.wrap(service);
 * }</pre>
 *
 * <p><b>Note:</b> This only works with interfaces. For concrete classes,
 * use AspectJ or create a wrapper class manually.</p>
 */
@Component
public class HumanPanicProxy implements InvocationHandler {

    private final Object target;
    @Autowired
    private static CrashReporter crashReporter;
    @Autowired
    private static UserNotifier userNotifier;

    /**
     * Private constructor - use static wrap() methods instead
     */
    private HumanPanicProxy(Object target) {
        this.target = target;
    }

    /**
     * Wraps an object with HumanPanic proxy.
     * The object must implement at least one interface.
     *
     * @param target the object to wrap
     * @param <T> the type of the object
     * @return a proxied instance that intercepts @HumanPanic annotated methods
     * @throws IllegalArgumentException if target doesn't implement any interfaces
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(T target) {
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
                new HumanPanicProxy(target)
        );
    }

    /**
     * Wraps an object with HumanPanic proxy for a specific interface.
     *
     * @param target the object to wrap
     * @param interfaceClass the interface to proxy
     * @param <T> the interface type
     * @return a proxied instance that intercepts @HumanPanic annotated methods
     * @throws IllegalArgumentException if target doesn't implement the interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(Object target, Class<T> interfaceClass) {
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
                new HumanPanicProxy(target)
        );
    }

    /**
     * Wraps an object with HumanPanic proxy for multiple interfaces.
     *
     * @param target the object to wrap
     * @param interfaces the interfaces to proxy
     * @param <T> the type of the object
     * @return a proxied instance that intercepts @HumanPanic annotated methods
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(T target, Class<?>... interfaces) {
        if (target == null) {
            throw new IllegalArgumentException("Target object cannot be null");
        }
        if (interfaces == null || interfaces.length == 0) {
            throw new IllegalArgumentException("Must provide at least one interface");
        }

        for (Class<?> iface : interfaces) {
            if (!iface.isInterface()) {
                throw new IllegalArgumentException(iface.getName() + " is not an interface");
            }
            if (!iface.isAssignableFrom(target.getClass())) {
                throw new IllegalArgumentException(
                        "Target must implement " + iface.getName()
                );
            }
        }

        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                interfaces,
                new HumanPanicProxy(target)
        );
    }

    /**
     * Checks if an object is already wrapped by HumanPanicProxy
     *
     * @param obj the object to check
     * @return true if the object is a HumanPanic proxy
     */
    public static boolean isWrapped(Object obj) {
        return Proxy.isProxyClass(obj.getClass())
                && Proxy.getInvocationHandler(obj) instanceof HumanPanicProxy;
    }

    /**
     * Unwraps a proxied object to get the original target
     *
     * @param proxy the proxy object
     * @param <T> the type of the target
     * @return the original target object
     * @throws IllegalArgumentException if object is not a HumanPanic proxy
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
        // Check if method has @HumanPanic annotation
        HumanPanic annotation = method.getAnnotation(HumanPanic.class);

        // If no annotation, just invoke normally
        if (annotation == null) {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        // Method has @HumanPanic, apply panic handling
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            handleException(cause, annotation, method);

            // Exit if configured
            if (annotation.exitCode() != 0) {
                System.exit(annotation.exitCode());
            }

            // Return default value based on return type
            return getDefaultReturnValue(method.getReturnType());
        } catch (Exception e) {
            // Handle reflection exceptions
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            handleException(cause, annotation, method);

            if (annotation.exitCode() != 0) {
                System.exit(annotation.exitCode());
            }

            return getDefaultReturnValue(method.getReturnType());
        }
    }

    /**
     * Handles the exception according to annotation settings
     */
    private void handleException(Throwable throwable, HumanPanic annotation, Method method) {
        // Create crash report if enabled
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

        // Print stack trace if debugging
        if (annotation.printStackTrace()) {
            throwable.printStackTrace();
        }
    }

    /**
     * Returns default value for primitive types, null for objects
     */
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
