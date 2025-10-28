package io.pants.humanpanic.unitTest.reporter;

import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.ConfigLoader;
import io.pants.humanpanic.reporter.UserNotifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserNotifier
 */
class UserNotifierTest {

    @Mock
    private ConfigLoader configLoader;

    @Mock
    private AppMetadata metadata;

    private UserNotifier userNotifier;
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        System.setErr(new PrintStream(errContent));

        when(configLoader.getMetadata()).thenReturn(metadata);
        when(metadata.getName()).thenReturn("Test App");
        when(metadata.getVersion()).thenReturn("1.0.0");
        when(metadata.getAuthors()).thenReturn(new String[]{"Test Author"});
        when(metadata.getHomepage()).thenReturn("https://test.com");
        when(metadata.getSupportUrl()).thenReturn("https://test.com/support");
        when(metadata.getIssueUrl()).thenReturn("https://test.com/issues");

        userNotifier = new UserNotifier(configLoader);
    }

    @AfterEach
    void restoreStreams() {
        System.setErr(originalErr);
    }

    @Test
    void testNotifyWithReport_BasicMessage() {
        String message = "Test error message";
        String reportPath = "/path/to/crash-report.json";

        userNotifier.notifyWithReport(message, reportPath);

        String output = errContent.toString();
        assertTrue(output.contains("Well, this is embarrassing"));
        assertTrue(output.contains("Test App"));
        assertTrue(output.contains(message));
        assertTrue(output.contains(reportPath));
    }

    @Test
    void testNotifyWithReport_WithoutCustomMessage() {
        String reportPath = "/path/to/crash-report.json";

        userNotifier.notifyWithReport("An error occurred", reportPath);

        String output = errContent.toString();
        assertTrue(output.contains("Well, this is embarrassing"));
        assertTrue(output.contains(reportPath));
        assertFalse(output.contains("An error occurred\n"));
    }

    @Test
    void testNotifyWithReport_WithNullReportPath() {
        String message = "Test error message";

        userNotifier.notifyWithReport(message, null);

        String output = errContent.toString();
        assertTrue(output.contains("Well, this is embarrassing"));
        assertTrue(output.contains(message));
        assertFalse(output.contains("We have generated a report file at"));
    }

    @Test
    void testNotifyWithReport_ContainsIssueUrl() {
        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String output = errContent.toString();
        assertTrue(output.contains("Open an issue at"));
        assertTrue(output.contains("https://test.com/issues"));
    }

    @Test
    void testNotifyWithReport_ContainsSupportUrl() {
        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String output = errContent.toString();
        assertTrue(output.contains("Get help at"));
        assertTrue(output.contains("https://test.com/support"));
    }

    @Test
    void testNotifyWithReport_ContainsAuthors() {
        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String output = errContent.toString();
        assertTrue(output.contains("Contact the authors"));
        assertTrue(output.contains("Test Author"));
    }

    @Test
    void testNotifyWithReport_WithEmptyIssueUrl() {
        when(metadata.getIssueUrl()).thenReturn("");

        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String output = errContent.toString();
        assertFalse(output.contains("Open an issue at"));
    }

    @Test
    void testNotifyWithReport_WithEmptySupportUrl() {
        when(metadata.getSupportUrl()).thenReturn("");

        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String output = errContent.toString();
        assertFalse(output.contains("Get help at"));
    }

    @Test
    void testNotifyWithReport_WithUnknownAuthors() {
        when(metadata.getAuthors()).thenReturn(new String[]{"Unknown"});

        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String output = errContent.toString();
        assertFalse(output.contains("Contact the authors"));
    }

    @Test
    void testNotifyWithReport_MultipleAuthors() {
        when(metadata.getAuthors()).thenReturn(new String[]{"Author1", "Author2", "Author3"});

        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String output = errContent.toString();
        assertTrue(output.contains("Author1"));
        assertTrue(output.contains("Author2"));
        assertTrue(output.contains("Author3"));
    }

    @Test
    void testNotify_BasicMessage() {
        String message = "Custom error message";
        Throwable throwable = new RuntimeException("Test exception");

        userNotifier.notify(message, throwable);

        String output = errContent.toString();
        assertTrue(output.contains("Well, this is embarrassing"));
        assertTrue(output.contains("Test App"));
        assertTrue(output.contains(message));
        assertTrue(output.contains("RuntimeException"));
        assertTrue(output.contains("Test exception"));
    }

    @Test
    void testNotify_WithoutCustomMessage() {
        Throwable throwable = new RuntimeException("Test exception");

        userNotifier.notify("An error occurred", throwable);

        String output = errContent.toString();
        assertTrue(output.contains("Well, this is embarrassing"));
        assertTrue(output.contains("RuntimeException"));
        assertFalse(output.contains("An error occurred\n  "));
    }

    @Test
    void testNotify_WithNullMessage() {
        Throwable throwable = new RuntimeException("Test exception");

        userNotifier.notify(null, throwable);

        String output = errContent.toString();
        assertTrue(output.contains("Well, this is embarrassing"));
        assertTrue(output.contains("RuntimeException"));
    }

    @Test
    void testNotify_WithEmptyMessage() {
        Throwable throwable = new RuntimeException("Test exception");

        userNotifier.notify("", throwable);

        String output = errContent.toString();
        assertTrue(output.contains("Well, this is embarrassing"));
        assertTrue(output.contains("RuntimeException"));
    }

    @Test
    void testNotify_WithThrowableWithoutMessage() {
        Throwable throwable = new RuntimeException((String) null);

        userNotifier.notify("Test message", throwable);

        String output = errContent.toString();
        assertTrue(output.contains("Well, this is embarrassing"));
        assertTrue(output.contains("Test message"));
        assertTrue(output.contains("RuntimeException"));
    }

    @Test
    void testNotify_DifferentExceptionTypes() {
        Exception[] exceptions = {
                new NullPointerException("NPE"),
                new IllegalArgumentException("IAE"),
                new IllegalStateException("ISE"),
                new IndexOutOfBoundsException("IOOBE")
        };

        for (Exception exception : exceptions) {
            errContent.reset();
            userNotifier.notify("Test", exception);

            String output = errContent.toString();
            assertTrue(output.contains(exception.getClass().getSimpleName()));
        }
    }

    @Test
    void testNotify_ContainsContinueMessage() {
        Throwable throwable = new RuntimeException("Test");

        userNotifier.notify("Test", throwable);

        String output = errContent.toString();
        assertTrue(output.contains("The application will continue running"));
    }

    @Test
    void testNotifyWithReport_ContainsPrivacyMessage() {
        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String output = errContent.toString();
        assertTrue(output.contains("We take privacy seriously"));
        assertTrue(output.contains("do not perform any automated error collection"));
    }

    @Test
    void testNotifyWithReport_ContainsThankYouMessage() {
        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String output = errContent.toString();
        assertTrue(output.contains("Thank you kindly!"));
    }

    @Test
    void testNotifyWithReport_ContainsCrashReportSubject() {
        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String output = errContent.toString();
        assertTrue(output.contains("Test App Crash Report"));
    }

    @Test
    void testNotify_WithEmptyAppName() {
        when(metadata.getName()).thenReturn("");

        userNotifier.notify("Test", new RuntimeException("Test"));

        String output = errContent.toString();
        assertTrue(output.contains("encountered an error"));
    }

    @Test
    void testNotifyWithReport_WithNullMessage() {
        userNotifier.notifyWithReport(null, "/path/to/report.json");

        String output = errContent.toString();
        assertTrue(output.contains("Well, this is embarrassing"));
        // Should not throw exception
    }

    @Test
    void testNotifyWithReport_CompleteScenario() {
        String message = "Database connection failed";
        String reportPath = "/var/logs/crash-2024-01-01-12-00-00.json";

        userNotifier.notifyWithReport(message, reportPath);

        String output = errContent.toString();

        // Verify all key components are present
        assertTrue(output.contains("Well, this is embarrassing"));
        assertTrue(output.contains("Test App"));
        assertTrue(output.contains(message));
        assertTrue(output.contains(reportPath));
        assertTrue(output.contains("Test App Crash Report"));
        assertTrue(output.contains("https://test.com/issues"));
        assertTrue(output.contains("https://test.com/support"));
        assertTrue(output.contains("Test Author"));
        assertTrue(output.contains("Thank you kindly!"));
    }
}
