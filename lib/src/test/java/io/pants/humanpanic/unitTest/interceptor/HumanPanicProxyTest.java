package io.pants.humanpanic.unitTest.interceptor;

import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.interceptor.HumanPanicProxy;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HumanPanicProxy
 */
class HumanPanicProxyTest {

    @Mock
    private CrashReporter crashReporter;

    @Mock
    private UserNotifier userNotifier;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testWrap_WithInterface() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        assertNotNull(wrapped);
        assertTrue(HumanPanicProxy.isWrapped(wrapped));
    }

    @Test
    void testWrap_WithNullTarget_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                HumanPanicProxy.wrap(null, crashReporter, userNotifier)
        );
    }

    @Test
    void testWrap_WithSpecificInterface() {
        TestServiceImpl service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, TestService.class,
                crashReporter, userNotifier);

        assertNotNull(wrapped);
        assertTrue(HumanPanicProxy.isWrapped(wrapped));
    }

    @Test
    void testWrap_WithInvalidInterface_ThrowsException() {
        TestServiceImpl service = new TestServiceImpl();

        assertThrows(IllegalArgumentException.class, () ->
                HumanPanicProxy.wrap(service, AnotherInterface.class, crashReporter, userNotifier)
        );
    }

    @Test
    void testWrap_WithNullInterface_ThrowsException() {
        TestServiceImpl service = new TestServiceImpl();

        assertThrows(IllegalArgumentException.class, () ->
                HumanPanicProxy.wrap(service, (Class<TestService>) null, crashReporter, userNotifier)
        );
    }

    @Test
    void testIsWrapped_WithWrappedObject_ReturnsTrue() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        assertTrue(HumanPanicProxy.isWrapped(wrapped));
    }

    @Test
    void testIsWrapped_WithNonWrappedObject_ReturnsFalse() {
        TestService service = new TestServiceImpl();

        assertFalse(HumanPanicProxy.isWrapped(service));
    }

    @Test
    void testUnwrap_ReturnsOriginalObject() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        TestService unwrapped = HumanPanicProxy.unwrap(wrapped);

        assertSame(service, unwrapped);
    }

    @Test
    void testUnwrap_WithNonWrappedObject_ThrowsException() {
        TestService service = new TestServiceImpl();

        assertThrows(IllegalArgumentException.class, () ->
                HumanPanicProxy.unwrap(service)
        );
    }

    @Test
    void testInvoke_MethodWithoutAnnotation_ExecutesNormally() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        String result = wrapped.normalMethod();

        assertEquals("Normal execution", result);
        verifyNoInteractions(crashReporter, userNotifier);
    }

    @Test
    void testInvoke_MethodWithAnnotation_HandlesException() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        when(crashReporter.createReport(any(), any())).thenReturn("/path/to/report.json");

        wrapped.methodThatThrows();

        verify(crashReporter).createReport(any(RuntimeException.class), any());
        verify(userNotifier).notifyWithReport(anyString(), eq("/path/to/report.json"));
    }

    @Test
    void testInvoke_MethodWithAnnotation_NoCrashReport() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        wrapped.methodWithoutCrashReport();

        verify(crashReporter, never()).createReport(any(), any());
        verify(userNotifier).notify(anyString(), any(RuntimeException.class));
    }

    @Test
    void testInvoke_SilentMethod_NoNotification() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        wrapped.silentMethod();

        verifyNoInteractions(userNotifier);
    }

    @Test
    void testInvoke_ReturnsDefaultForInt() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        int result = wrapped.methodReturningInt();

        assertEquals(0, result);
    }

    @Test
    void testInvoke_ReturnsDefaultForBoolean() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        boolean result = wrapped.methodReturningBoolean();

        assertFalse(result);
    }

    @Test
    void testInvoke_ReturnsNullForObject() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        String result = wrapped.methodReturningString();

        assertNull(result);
    }

    @Test
    void testInvoke_WithCustomMessage() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        when(crashReporter.createReport(any(), any())).thenReturn("/path/to/report.json");

        wrapped.methodWithCustomMessage();

        verify(userNotifier).notifyWithReport(eq("Custom error"), anyString());
    }

    @Test
    void testInvoke_WithEmptyMessage() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        when(crashReporter.createReport(any(), any())).thenReturn("/path/to/report.json");

        wrapped.methodWithEmptyMessage();

        verify(userNotifier).notifyWithReport(eq("An error occurred"), anyString());
    }

    @Test
    void testCreateProxy_WithProxyComponent() {
        HumanPanicProxy proxyComponent = new HumanPanicProxy(crashReporter, userNotifier);
        TestService service = new TestServiceImpl();

        TestService wrapped = proxyComponent.createProxy(service);

        assertNotNull(wrapped);
        assertTrue(HumanPanicProxy.isWrapped(wrapped));
    }

    @Test
    void testInvoke_MethodWithArguments() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        String result = wrapped.methodWithArgs("test", 42);

        assertEquals("test-42", result);
    }

    @Test
    void testInvoke_VoidMethod() {
        TestService service = new TestServiceImpl();
        TestService wrapped = HumanPanicProxy.wrap(service, crashReporter, userNotifier);

        assertDoesNotThrow(() -> wrapped.voidMethod());
    }

    // Test interfaces and implementations
    public interface TestService {
        String normalMethod();

        @HumanPanic(createCrashReport = true)
        void methodThatThrows();

        @HumanPanic(createCrashReport = false)
        void methodWithoutCrashReport();

        @HumanPanic(silent = true)
        void silentMethod();

        @HumanPanic
        int methodReturningInt();

        @HumanPanic
        boolean methodReturningBoolean();

        @HumanPanic
        String methodReturningString();

        @HumanPanic(message = "Custom error")
        void methodWithCustomMessage();

        @HumanPanic(message = "")
        void methodWithEmptyMessage();

        String methodWithArgs(String arg1, int arg2);

        void voidMethod();
    }

    public static class TestServiceImpl implements TestService {
        @Override
        public String normalMethod() {
            return "Normal execution";
        }

        @Override
        public void methodThatThrows() {
            throw new RuntimeException("Test exception");
        }

        @Override
        public void methodWithoutCrashReport() {
            throw new RuntimeException("Test exception");
        }

        @Override
        public void silentMethod() {
            throw new RuntimeException("Test exception");
        }

        @Override
        public int methodReturningInt() {
            throw new RuntimeException("Test exception");
        }

        @Override
        public boolean methodReturningBoolean() {
            throw new RuntimeException("Test exception");
        }

        @Override
        public String methodReturningString() {
            throw new RuntimeException("Test exception");
        }

        @Override
        public void methodWithCustomMessage() {
            throw new RuntimeException("Test exception");
        }

        @Override
        public void methodWithEmptyMessage() {
            throw new RuntimeException("Test exception");
        }

        @Override
        public String methodWithArgs(String arg1, int arg2) {
            return arg1 + "-" + arg2;
        }

        @Override
        public void voidMethod() {
            // Does nothing
        }
    }

    public interface AnotherInterface {
        void anotherMethod();
    }
}
