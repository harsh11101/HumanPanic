package io.pants.humanpanic.interceptor;

import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HumanPanicProxyBehaviourTest {

    public interface Service {
        int compute();

        String plain() throws IOException;

        @HumanPanic(message = "from interface")
        void annotatedOnInterface();

        void interrupted() throws InterruptedException;
    }

    public static class ServiceImpl implements Service {
        @HumanPanic(message = "from implementation")
        @Override
        public int compute() {
            throw new IllegalStateException("boom");
        }

        @Override
        public String plain() throws IOException {
            throw new IOException("checked and unannotated");
        }

        @Override
        public void annotatedOnInterface() {
            throw new IllegalStateException("iface");
        }

        @HumanPanic
        @Override
        public void interrupted() throws InterruptedException {
            throw new InterruptedException();
        }
    }

    /** Inherits its interface from the superclass. */
    public static class SubService extends ServiceImpl {
    }

    private CrashReporter reporter;
    private UserNotifier notifier;

    @BeforeEach
    void setUp() {
        reporter = mock(CrashReporter.class);
        notifier = mock(UserNotifier.class);
        when(reporter.createReport(any(), any())).thenReturn("/r.json");
    }

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    void annotationOnImplementationIsHonoured() throws Exception {
        // Regression (B2): only the interface method used to be inspected.
        Service proxy = HumanPanicProxy.wrap(new ServiceImpl(), Service.class, reporter, notifier);

        assertEquals(0, proxy.compute());

        verify(notifier).notifyWithReport(eq("from implementation"), eq("/r.json"));
        verify(reporter).createReport(any(IllegalStateException.class),
                eq(ServiceImpl.class.getMethod("compute")));
    }

    @Test
    void annotationOnInterfaceStillWorks() {
        Service proxy = HumanPanicProxy.wrap(new ServiceImpl(), Service.class, reporter, notifier);

        proxy.annotatedOnInterface();

        verify(notifier).notifyWithReport(eq("from interface"), any());
    }

    @Test
    void unannotatedMethodPropagatesTheOriginalCheckedException() {
        Service proxy = HumanPanicProxy.wrap(new ServiceImpl(), Service.class, reporter, notifier);

        IOException e = assertThrows(IOException.class, proxy::plain);

        assertEquals("checked and unannotated", e.getMessage());
        verifyNoInteractions(reporter, notifier);
    }

    @Test
    void interfacesInheritedFromSuperclassAreProxied() {
        // Regression (B14)
        Service proxy = HumanPanicProxy.wrap(new SubService(), reporter, notifier);

        assertEquals(0, proxy.compute());
    }

    @Test
    void isWrappedAcceptsNull() {
        // Regression (B15)
        assertFalse(HumanPanicProxy.isWrapped(null));
        assertFalse(HumanPanicProxy.isWrapped(Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{Runnable.class}, (p, m, a) -> null)));
    }

    @Test
    void unboundFactoryRefusesToActAsHandler() {
        HumanPanicProxy factory = new HumanPanicProxy(reporter, notifier);

        assertThrows(IllegalStateException.class,
                () -> factory.invoke(null, Object.class.getMethod("hashCode"), null));
    }

    @Test
    void createProxyDoesNotRebindTheFactory() {
        HumanPanicProxy factory = new HumanPanicProxy(reporter, notifier);
        ServiceImpl first = new ServiceImpl();
        ServiceImpl second = new ServiceImpl();

        Service p1 = factory.createProxy((Service) first);
        Service p2 = factory.createProxy((Service) second);

        assertSame(first, HumanPanicProxy.unwrap(p1));
        assertSame(second, HumanPanicProxy.unwrap(p2));
    }

    @Test
    void equalsIsReflexiveThroughTheProxy() {
        Service proxy = HumanPanicProxy.wrap(new ServiceImpl(), Service.class, reporter, notifier);

        assertEquals(proxy, proxy);
        assertEquals(proxy.hashCode(), HumanPanicProxy.unwrap(proxy).hashCode());
    }

    @Test
    void interruptedExceptionRestoresFlag() throws Exception {
        Service proxy = HumanPanicProxy.wrap(new ServiceImpl(), Service.class, reporter, notifier);

        proxy.interrupted();

        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void reporterFailureDoesNotEscapeTheProxy() {
        when(reporter.createReport(any(), any())).thenThrow(new IllegalStateException("reporter bug"));
        Service proxy = HumanPanicProxy.wrap(new ServiceImpl(), Service.class, reporter, notifier);

        assertEquals(0, proxy.compute());
    }

    @Test
    void wrapAndUnwrapRejectInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> HumanPanicProxy.wrap(null, reporter, notifier));
        assertThrows(IllegalArgumentException.class,
                () -> HumanPanicProxy.wrap(null, Service.class, reporter, notifier));
        assertThrows(IllegalArgumentException.class,
                () -> HumanPanicProxy.wrap(new ServiceImpl(), null, reporter, notifier));
        assertThrows(IllegalArgumentException.class,
                () -> HumanPanicProxy.wrap(new Object(), Service.class, reporter, notifier));
        assertThrows(IllegalArgumentException.class, () -> HumanPanicProxy.unwrap(new ServiceImpl()));
    }

    @Test
    void wrapRejectsConcreteClassAsInterface() {
        assertThrows(IllegalArgumentException.class,
                () -> HumanPanicProxy.wrap(new ServiceImpl(), ServiceImpl.class, reporter, notifier));
    }
}
