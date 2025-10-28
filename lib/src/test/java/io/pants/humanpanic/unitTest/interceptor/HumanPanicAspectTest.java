package io.pants.humanpanic.unitTest.interceptor;

import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.interceptor.HumanPanicAspect;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HumanPanicAspect
 */
class HumanPanicAspectTest {

    @Mock
    private CrashReporter crashReporter;

    @Mock
    private UserNotifier userNotifier;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private HumanPanicAspect aspect;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aspect = new HumanPanicAspect(crashReporter, userNotifier);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
    }

    @Test
    void testHandlePanic_SuccessfulExecution() throws Throwable {
        Object expectedResult = "Success";
        when(joinPoint.proceed()).thenReturn(expectedResult);

        Object result = aspect.handlePanic(joinPoint);

        assertEquals(expectedResult, result);
        verify(joinPoint).proceed();
        verifyNoInteractions(crashReporter, userNotifier);
    }

    @Test
    void testHandlePanic_WithException_CreatesCrashReport() throws Throwable {
        Method method = TestClass.class.getMethod("methodWithCrashReport");
        HumanPanic annotation = method.getAnnotation(HumanPanic.class);
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getReturnType()).thenReturn(void.class);
        when(crashReporter.createReport(any(), any())).thenReturn("/path/to/report.json");

        Object result = aspect.handlePanic(joinPoint);

        assertNull(result);
        verify(crashReporter).createReport(exception, method);
        verify(userNotifier).notifyWithReport(anyString(), eq("/path/to/report.json"));
    }

    @Test
    void testHandlePanic_WithException_NoCrashReport() throws Throwable {
        Method method = TestClass.class.getMethod("methodWithoutCrashReport");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getReturnType()).thenReturn(void.class);

        Object result = aspect.handlePanic(joinPoint);

        assertNull(result);
        verify(crashReporter, never()).createReport(any(), any());
        verify(userNotifier).notify(anyString(), eq(exception));
    }

    @Test
    void testHandlePanic_SilentMode() throws Throwable {
        Method method = TestClass.class.getMethod("methodSilent");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getReturnType()).thenReturn(void.class);

        Object result = aspect.handlePanic(joinPoint);

        assertNull(result);
        verifyNoInteractions(userNotifier);
    }

    @Test
    void testHandlePanic_WithPrintStackTrace() throws Throwable {
        Method method = TestClass.class.getMethod("methodWithStackTrace");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getReturnType()).thenReturn(void.class);

        aspect.handlePanic(joinPoint);

        // Stack trace should be printed - difficult to verify, but method should complete
        verify(crashReporter, never()).createReport(any(), any());
    }

    @Test
    void testHandlePanic_WithCustomMessage() throws Throwable {
        Method method = TestClass.class.getMethod("methodWithCustomMessage");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getReturnType()).thenReturn(void.class);
        when(crashReporter.createReport(any(), any())).thenReturn("/path/to/report.json");

        aspect.handlePanic(joinPoint);

        verify(userNotifier).notifyWithReport(eq("Custom error message"), anyString());
    }

    @Test
    void testHandlePanic_ReturnsDefaultForPrimitives() throws Throwable {
        Method intMethod = TestClass.class.getMethod("methodReturningInt");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(intMethod);
        when(methodSignature.getReturnType()).thenReturn(int.class);

        Object result = aspect.handlePanic(joinPoint);

        assertEquals(0, result);
    }

    @Test
    void testHandlePanic_ReturnsDefaultForBoolean() throws Throwable {
        Method booleanMethod = TestClass.class.getMethod("methodReturningBoolean");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(booleanMethod);
        when(methodSignature.getReturnType()).thenReturn(boolean.class);

        Object result = aspect.handlePanic(joinPoint);

        assertEquals(false, result);
    }

    @Test
    void testHandlePanic_ReturnsDefaultForLong() throws Throwable {
        Method longMethod = TestClass.class.getMethod("methodReturningLong");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(longMethod);
        when(methodSignature.getReturnType()).thenReturn(long.class);

        Object result = aspect.handlePanic(joinPoint);

        assertEquals(0L, result);
    }

    @Test
    void testHandlePanic_ReturnsDefaultForFloat() throws Throwable {
        Method floatMethod = TestClass.class.getMethod("methodReturningFloat");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(floatMethod);
        when(methodSignature.getReturnType()).thenReturn(float.class);

        Object result = aspect.handlePanic(joinPoint);

        assertEquals(0.0f, result);
    }

    @Test
    void testHandlePanic_ReturnsDefaultForDouble() throws Throwable {
        Method doubleMethod = TestClass.class.getMethod("methodReturningDouble");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(doubleMethod);
        when(methodSignature.getReturnType()).thenReturn(double.class);

        Object result = aspect.handlePanic(joinPoint);

        assertEquals(0.0d, result);
    }

    @Test
    void testHandlePanic_ReturnsDefaultForChar() throws Throwable {
        Method charMethod = TestClass.class.getMethod("methodReturningChar");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(charMethod);
        when(methodSignature.getReturnType()).thenReturn(char.class);

        Object result = aspect.handlePanic(joinPoint);

        assertEquals('\u0000', result);
    }

    @Test
    void testHandlePanic_ReturnsDefaultForByte() throws Throwable {
        Method byteMethod = TestClass.class.getMethod("methodReturningByte");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(byteMethod);
        when(methodSignature.getReturnType()).thenReturn(byte.class);

        Object result = aspect.handlePanic(joinPoint);

        assertEquals((byte) 0, result);
    }

    @Test
    void testHandlePanic_ReturnsDefaultForShort() throws Throwable {
        Method shortMethod = TestClass.class.getMethod("methodReturningShort");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(shortMethod);
        when(methodSignature.getReturnType()).thenReturn(short.class);

        Object result = aspect.handlePanic(joinPoint);

        assertEquals((short) 0, result);
    }

    @Test
    void testHandlePanic_ReturnsNullForObjects() throws Throwable {
        Method stringMethod = TestClass.class.getMethod("methodReturningString");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(stringMethod);
        when(methodSignature.getReturnType()).thenReturn(String.class);

        Object result = aspect.handlePanic(joinPoint);

        assertNull(result);
    }

    @Test
    void testHandlePanic_ReturnsNullForVoid() throws Throwable {
        Method voidMethod = TestClass.class.getMethod("methodWithCrashReport");
        RuntimeException exception = new RuntimeException("Test exception");

        when(joinPoint.proceed()).thenThrow(exception);
        when(methodSignature.getMethod()).thenReturn(voidMethod);
        when(methodSignature.getReturnType()).thenReturn(void.class);
        when(crashReporter.createReport(any(), any())).thenReturn("/path/to/report.json");

        Object result = aspect.handlePanic(joinPoint);

        assertNull(result);
    }

    @Test
    void testHandlePanic_DifferentExceptionTypes() throws Throwable {
        Method method = TestClass.class.getMethod("methodWithCrashReport");
        when(methodSignature.getMethod()).thenReturn(method);
        when(methodSignature.getReturnType()).thenReturn(void.class);
        when(crashReporter.createReport(any(), any())).thenReturn("/path/to/report.json");

        Exception[] exceptions = {
                new NullPointerException("NPE"),
                new IllegalArgumentException("IAE"),
                new IllegalStateException("ISE"),
                new IndexOutOfBoundsException("IOOBE")
        };

        for (Exception exception : exceptions) {
            doThrow(exception).when(joinPoint).proceed();
            aspect.handlePanic(joinPoint);
            verify(crashReporter).createReport(eq(exception), eq(method));
        }
    }

    // Test class with annotated methods
    public static class TestClass {
        @HumanPanic(createCrashReport = true)
        public void methodWithCrashReport() {}

        @HumanPanic(createCrashReport = false)
        public void methodWithoutCrashReport() {}

        @HumanPanic(silent = true)
        public void methodSilent() {}

        @HumanPanic(printStackTrace = true, createCrashReport = false)
        public void methodWithStackTrace() {}

        @HumanPanic(message = "Custom error message")
        public void methodWithCustomMessage() {}

        @HumanPanic
        public int methodReturningInt() { return 0; }

        @HumanPanic
        public boolean methodReturningBoolean() { return false; }

        @HumanPanic
        public long methodReturningLong() { return 0L; }

        @HumanPanic
        public float methodReturningFloat() { return 0.0f; }

        @HumanPanic
        public double methodReturningDouble() { return 0.0d; }

        @HumanPanic
        public char methodReturningChar() { return '\u0000'; }

        @HumanPanic
        public byte methodReturningByte() { return 0; }

        @HumanPanic
        public short methodReturningShort() { return 0; }

        @HumanPanic
        public String methodReturningString() { return null; }
    }
}
