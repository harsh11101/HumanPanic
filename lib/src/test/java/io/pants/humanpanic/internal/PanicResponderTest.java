package io.pants.humanpanic.internal;

import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PanicResponderTest {

    private CrashReporter reporter;
    private UserNotifier notifier;
    private final List<Integer> exits = new ArrayList<>();
    private PanicResponder responder;

    @BeforeEach
    void setUp() {
        reporter = mock(CrashReporter.class);
        notifier = mock(UserNotifier.class);
        when(reporter.createReport(any(), any())).thenReturn("/r.json");
        responder = new PanicResponder(reporter, notifier, exits::add);
    }

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    private static HumanPanic options(String method) throws NoSuchMethodException {
        return Annotated.class.getMethod(method).getAnnotation(HumanPanic.class);
    }

    @Test
    void nullOptionsMeanAnnotationDefaults() {
        RuntimeException failure = new RuntimeException("x");

        assertEquals("/r.json", responder.report(failure, null, null));

        verify(reporter).createReport(failure, null);
        verify(notifier).notifyWithReport("", "/r.json");
        verify(notifier, never()).printStackTrace(any());
    }

    @Test
    void silentStillWritesReport() throws Exception {
        responder.report(new RuntimeException(), options("silent"), null);

        verify(reporter).createReport(any(), any());
        verifyNoInteractions(notifier);
    }

    @Test
    void noReportShowsShortMessage() throws Exception {
        RuntimeException failure = new RuntimeException();

        assertNull(responder.report(failure, options("noReport"), null));

        verify(reporter, never()).createReport(any(), any());
        verify(notifier).notify("short", failure);
    }

    @Test
    void reportFailureIsPassedToNotifierAsNullPath() {
        when(reporter.createReport(any(), any())).thenReturn(null);

        responder.report(new RuntimeException(), null, null);

        verify(notifier).notifyWithReport("", null);
    }

    @Test
    void reporterExceptionNeverEscapes() throws Throwable {
        // Regression (B4): a failure inside HumanPanic must not replace the application's exception.
        when(reporter.createReport(any(), any())).thenThrow(new IllegalStateException("reporter bug"));

        assertNull(responder.recover(new RuntimeException("app"), null, null, String.class));
    }

    @Test
    void notifierExceptionNeverEscapes() {
        doThrow(new IllegalStateException("broken stream")).when(notifier).notifyWithReport(any(), any());

        assertDoesNotThrow(() -> responder.report(new RuntimeException(), null, null));
    }

    @Test
    void linkageErrorWhileReportingIsContained() {
        when(reporter.createReport(any(), any())).thenThrow(new NoClassDefFoundError("missing"));

        assertDoesNotThrow(() -> responder.report(new RuntimeException(), null, null));
    }

    @Test
    void exitCodeInvokesExitAfterReporting() throws Throwable {
        responder.recover(new RuntimeException(), options("exits"), null, void.class);

        assertEquals(List.of(3), exits);
        verify(notifier).notifyWithReport(anyString(), any());
    }

    @Test
    void zeroExitCodeDoesNotExit() throws Throwable {
        responder.recover(new RuntimeException(), null, null, void.class);

        assertTrue(exits.isEmpty());
    }

    @Test
    void virtualMachineErrorIsReportedThenRethrown() {
        // Regression (B12): the JVM must not silently continue after OOM / StackOverflowError.
        StackOverflowError error = new StackOverflowError();

        StackOverflowError thrown = assertThrows(StackOverflowError.class,
                () -> responder.recover(error, null, null, int.class));

        assertSame(error, thrown);
        verify(reporter).createReport(error, null);
    }

    @Test
    void interruptedExceptionRestoresInterruptFlag() throws Throwable {
        // Regression (B11)
        assertFalse(Thread.currentThread().isInterrupted());

        responder.recover(new InterruptedException(), null, null, void.class);

        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void printStackTraceGoesThroughNotifier() throws Exception {
        RuntimeException failure = new RuntimeException();

        responder.report(failure, options("trace"), null);

        verify(notifier).printStackTrace(failure);
    }

    @Test
    void customMessageIsPassedVerbatim() throws Exception {
        responder.report(new RuntimeException(), options("message"), null);

        verify(notifier).notifyWithReport("An error occurred", "/r.json");
    }

    @Test
    void originMethodIsForwardedToReporter() throws Exception {
        Method origin = Annotated.class.getMethod("message");
        RuntimeException failure = new RuntimeException();

        responder.report(failure, null, origin);

        verify(reporter).createReport(failure, origin);
    }

    @Test
    void constructorRejectsNulls() {
        assertThrows(NullPointerException.class, () -> new PanicResponder(null, notifier));
        assertThrows(NullPointerException.class, () -> new PanicResponder(reporter, null));
        assertThrows(NullPointerException.class, () -> new PanicResponder(reporter, notifier, null));
    }

    @ParameterizedTest
    @ValueSource(classes = {boolean.class, byte.class, short.class, int.class, long.class,
            float.class, double.class, char.class})
    void defaultValueOfPrimitivesIsZeroLike(Class<?> type) {
        Object value = PanicResponder.defaultValue(type);
        assertNotNull(value);
        assertEquals(java.lang.reflect.Array.get(java.lang.reflect.Array.newInstance(type, 1), 0), value);
    }

    @Test
    void defaultValueOfReferencesAndVoidIsNull() {
        assertNull(PanicResponder.defaultValue(void.class));
        assertNull(PanicResponder.defaultValue(Void.class));
        assertNull(PanicResponder.defaultValue(String.class));
        assertNull(PanicResponder.defaultValue(Integer.class));
        assertNull(PanicResponder.defaultValue(null));
    }

    public static class Annotated {
        @HumanPanic(silent = true) public void silent() { }
        @HumanPanic(createCrashReport = false, message = "short") public void noReport() { }
        @HumanPanic(exitCode = 3) public void exits() { }
        @HumanPanic(printStackTrace = true) public void trace() { }
        @HumanPanic(message = "An error occurred") public void message() { }
    }
}
