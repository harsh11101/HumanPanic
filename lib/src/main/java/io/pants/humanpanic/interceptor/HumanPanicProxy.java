package io.pants.humanpanic.interceptor;

import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.internal.PanicResponder;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <b>Framework-free integration.</b> A JDK dynamic proxy that applies {@link HumanPanic} handling
 * to calls made through an interface.
 *
 * <p>{@code @HumanPanic} is looked up on the implementation's method first, then on the interface
 * method. Methods without the annotation pass through unchanged, and so do their exceptions.
 *
 * <p>Usually you create proxies with {@link io.pants.humanpanic.PanicHandler#wrap(Object, Class)}.
 * The static factories below are for callers that build the reporter and notifier themselves.
 *
 * <p>Only interfaces can be proxied. Proxies are thread-safe if the target is.
 */
public class HumanPanicProxy implements InvocationHandler {

    private final Object target;
    private final CrashReporter crashReporter;
    private final UserNotifier userNotifier;
    private final PanicResponder responder;
    private final ConcurrentMap<Method, Resolved> resolved = new ConcurrentHashMap<>();

    /**
     * Creates an unbound factory, used through {@link #createProxy(Object)}. An unbound instance
     * cannot itself be used as an {@link InvocationHandler}.
     *
     * @param crashReporter writes crash reports; must not be {@code null}
     * @param userNotifier  writes user messages; must not be {@code null}
     */
    public HumanPanicProxy(CrashReporter crashReporter, UserNotifier userNotifier) {
        this(null, crashReporter, userNotifier);
    }

    private HumanPanicProxy(Object target, CrashReporter crashReporter, UserNotifier userNotifier) {
        this.target = target;
        this.crashReporter = Objects.requireNonNull(crashReporter, "crashReporter");
        this.userNotifier = Objects.requireNonNull(userNotifier, "userNotifier");
        this.responder = new PanicResponder(crashReporter, userNotifier);
    }

    /**
     * Wraps {@code target} in a proxy for all of its interfaces, using this factory's reporter and
     * notifier. This instance is not modified.
     *
     * @param target the object to wrap
     * @param <T>    the target type
     * @return the proxy
     * @see #wrap(Object, CrashReporter, UserNotifier)
     */
    public <T> T createProxy(T target) {
        return wrap(target, crashReporter, userNotifier);
    }

    /**
     * Wraps an object in a proxy implementing <em>all</em> of its interfaces, including those
     * inherited from superclasses.
     *
     * <p>The returned object only implements those interfaces. If {@code T} is a class type, the
     * assignment at the call site throws a {@link ClassCastException}. Prefer
     * {@link #wrap(Object, Class, CrashReporter, UserNotifier)}.
     *
     * @param target        the object to wrap; must implement at least one interface
     * @param crashReporter writes crash reports
     * @param userNotifier  writes user messages
     * @param <T>           the (interface) type expected by the caller
     * @return the proxy
     * @throws IllegalArgumentException if {@code target} is {@code null} or implements no interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrap(T target, CrashReporter crashReporter, UserNotifier userNotifier) {
        if (target == null) {
            throw new IllegalArgumentException("Target object cannot be null");
        }
        Class<?>[] interfaces = allInterfaces(target.getClass());
        if (interfaces.length == 0) {
            throw new IllegalArgumentException(
                    "Target must implement at least one interface. "
                            + "Use wrap(target, interfaceClass, ...) or Spring AOP for concrete classes.");
        }
        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                interfaces,
                new HumanPanicProxy(target, crashReporter, userNotifier));
    }

    /**
     * Wraps an object in a proxy for one specific interface.
     *
     * @param target         the object to wrap
     * @param interfaceClass the interface to expose; {@code target} must implement it
     * @param crashReporter  writes crash reports
     * @param userNotifier   writes user messages
     * @param <T>            the interface type
     * @return the proxy
     * @throws IllegalArgumentException if an argument is {@code null}, {@code interfaceClass} is not
     *                                  an interface, or {@code target} does not implement it
     */
    public static <T> T wrap(Object target, Class<T> interfaceClass,
                             CrashReporter crashReporter, UserNotifier userNotifier) {
        if (target == null) {
            throw new IllegalArgumentException("Target object cannot be null");
        }
        if (interfaceClass == null || !interfaceClass.isInterface()) {
            throw new IllegalArgumentException("Must provide a valid interface class");
        }
        if (!interfaceClass.isInstance(target)) {
            throw new IllegalArgumentException("Target must implement " + interfaceClass.getName());
        }
        return interfaceClass.cast(Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new HumanPanicProxy(target, crashReporter, userNotifier)));
    }

    /**
     * Tells whether an object is a HumanPanic proxy.
     *
     * @param obj any object, or {@code null}
     * @return {@code true} if {@code obj} was created by this class
     */
    public static boolean isWrapped(Object obj) {
        return obj != null
                && Proxy.isProxyClass(obj.getClass())
                && Proxy.getInvocationHandler(obj) instanceof HumanPanicProxy;
    }

    /**
     * Returns the object wrapped by a HumanPanic proxy.
     *
     * @param proxy a proxy created by this class
     * @param <T>   the expected target type
     * @return the original target
     * @throws IllegalArgumentException if {@code proxy} is not a HumanPanic proxy
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(Object proxy) {
        if (!isWrapped(proxy)) {
            throw new IllegalArgumentException("Object is not a HumanPanic proxy");
        }
        return (T) ((HumanPanicProxy) Proxy.getInvocationHandler(proxy)).target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (target == null) {
            throw new IllegalStateException(
                    "This HumanPanicProxy is an unbound factory; use createProxy(target) or wrap(...)");
        }
        if (isEquals(method)) {
            Object other = args[0];
            return target.equals(isWrapped(other) ? unwrap(other) : other);
        }

        Resolved r = resolved.computeIfAbsent(method, this::resolve);
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable failure = e.getCause();
            if (r.annotation == null) {
                throw failure;
            }
            return responder.recover(failure, r.annotation, r.origin, method.getReturnType());
        }
        // IllegalAccessException / IllegalArgumentException mean a wiring bug, not an application
        // failure, so they propagate unchanged.
    }

    private Resolved resolve(Method interfaceMethod) {
        if (!interfaceMethod.canAccess(target)) {
            interfaceMethod.trySetAccessible();
        }
        Method implementation = null;
        try {
            implementation = target.getClass().getMethod(
                    interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (NoSuchMethodException | SecurityException ignored) {
            // e.g. an inherited default method; fall back to the interface method
        }
        HumanPanic annotation = Optional.ofNullable(implementation)
                .map(m -> m.getAnnotation(HumanPanic.class))
                .orElseGet(() -> interfaceMethod.getAnnotation(HumanPanic.class));
        return new Resolved(annotation, implementation != null ? implementation : interfaceMethod);
    }

    private static boolean isEquals(Method m) {
        return m.getName().equals("equals")
                && m.getParameterCount() == 1
                && m.getParameterTypes()[0] == Object.class;
    }

    private static Class<?>[] allInterfaces(Class<?> type) {
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                interfaces.add(i);
            }
        }
        return interfaces.toArray(new Class<?>[0]);
    }

    private static final class Resolved {
        final HumanPanic annotation;
        final Method origin;

        Resolved(HumanPanic annotation, Method origin) {
            this.annotation = annotation;
            this.origin = origin;
        }
    }
}
