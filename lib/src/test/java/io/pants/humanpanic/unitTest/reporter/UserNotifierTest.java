package io.pants.humanpanic.unitTest.reporter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.ConfigLoader;
import io.pants.humanpanic.reporter.UserNotifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserNotifier using Logback logger capture.
 */
class UserNotifierTest {

    @Mock
    private ConfigLoader configLoader;

    @Mock
    private AppMetadata metadata;

    private UserNotifier userNotifier;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup mock configuration
        when(configLoader.getMetadata()).thenReturn(metadata);
        when(metadata.getName()).thenReturn("Test App");
        when(metadata.getVersion()).thenReturn("1.0.0");
        when(metadata.getAuthors()).thenReturn(new String[]{"Test Author"});
        when(metadata.getHomepage()).thenReturn("https://test.com");
        when(metadata.getSupportUrl()).thenReturn("https://test.com/support");
        when(metadata.getIssueUrl()).thenReturn("https://test.com/issues");

        // Initialize logger capture
        logger = (Logger) LoggerFactory.getLogger(UserNotifier.class);
        logger.detachAndStopAllAppenders();
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);

        userNotifier = new UserNotifier(configLoader);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    private String getAllLogs() {
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
    }

    // ------------------ TESTS ------------------

    @Test
    void testNotifyWithReport_BasicMessage() {
        userNotifier.notifyWithReport("Test error message", "/path/to/crash-report.json");

        String logs = getAllLogs();
        assertTrue(logs.contains("Well, this is embarrassing"));
        assertTrue(logs.contains("Test App"));
        assertTrue(logs.contains("Test error message"));
        assertTrue(logs.contains("/path/to/crash-report.json"));
    }

    @Test
    void testNotifyWithReport_WithoutCustomMessage() {
        userNotifier.notifyWithReport("An error occurred", "/path/to/crash-report.json");

        String logs = getAllLogs();
        assertTrue(logs.contains("Well, this is embarrassing"));
        assertTrue(logs.contains("/path/to/crash-report.json"));
        assertFalse(logs.contains("An error occurred\n"));
    }

    @Test
    void testNotifyWithReport_WithNullReportPath() {
        userNotifier.notifyWithReport("Test error message", null);

        String logs = getAllLogs();
        assertTrue(logs.contains("Well, this is embarrassing"));
        assertTrue(logs.contains("Test error message"));
        assertFalse(logs.contains("We have generated a report file at"));
    }

    @Test
    void testNotifyWithReport_ContainsIssueUrl() {
        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String logs = getAllLogs();
        assertTrue(logs.contains("Open an issue at"));
        assertTrue(logs.contains("https://test.com/issues"));
    }

    @Test
    void testNotifyWithReport_ContainsSupportUrl() {
        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String logs = getAllLogs();
        assertTrue(logs.contains("Get help at"));
        assertTrue(logs.contains("https://test.com/support"));
    }

    @Test
    void testNotifyWithReport_ContainsAuthors() {
        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String logs = getAllLogs();
        assertTrue(logs.contains("Contact the authors"));
        assertTrue(logs.contains("Test Author"));
    }

    @Test
    void testNotifyWithReport_WithEmptyIssueUrl() {
        when(metadata.getIssueUrl()).thenReturn("");

        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String logs = getAllLogs();
        assertFalse(logs.contains("Open an issue at"));
    }

    @Test
    void testNotifyWithReport_WithEmptySupportUrl() {
        when(metadata.getSupportUrl()).thenReturn("");

        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String logs = getAllLogs();
        assertFalse(logs.contains("Get help at"));
    }

    @Test
    void testNotifyWithReport_WithUnknownAuthors() {
        when(metadata.getAuthors()).thenReturn(new String[]{"Unknown"});

        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String logs = getAllLogs();
        assertFalse(logs.contains("Contact the authors"));
    }

    @Test
    void testNotifyWithReport_MultipleAuthors() {
        when(metadata.getAuthors()).thenReturn(new String[]{"Author1", "Author2", "Author3"});

        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String logs = getAllLogs();
        assertTrue(logs.contains("Author1"));
        assertTrue(logs.contains("Author2"));
        assertTrue(logs.contains("Author3"));
    }

    @Test
    void testNotify_BasicMessage() {
        Throwable throwable = new RuntimeException("Test exception");

        userNotifier.notify("Custom error message", throwable);

        String logs = getAllLogs();
        assertTrue(logs.contains("Well, this is embarrassing"));
        assertTrue(logs.contains("Test App"));
        assertTrue(logs.contains("Custom error message"));
        assertTrue(logs.contains("RuntimeException"));
        assertTrue(logs.contains("Test exception"));
    }

    @Test
    void testNotify_WithoutCustomMessage() {
        Throwable throwable = new RuntimeException("Test exception");

        userNotifier.notify("An error occurred", throwable);

        String logs = getAllLogs();
        assertTrue(logs.contains("Well, this is embarrassing"));
        assertTrue(logs.contains("RuntimeException"));
        assertFalse(logs.contains("An error occurred\n  "));
    }

    @Test
    void testNotify_WithNullMessage() {
        Throwable throwable = new RuntimeException("Test exception");

        userNotifier.notify(null, throwable);

        String logs = getAllLogs();
        assertTrue(logs.contains("Well, this is embarrassing"));
        assertTrue(logs.contains("RuntimeException"));
    }

    @Test
    void testNotify_WithEmptyMessage() {
        Throwable throwable = new RuntimeException("Test exception");

        userNotifier.notify("", throwable);

        String logs = getAllLogs();
        assertTrue(logs.contains("Well, this is embarrassing"));
        assertTrue(logs.contains("RuntimeException"));
    }

    @Test
    void testNotify_WithThrowableWithoutMessage() {
        Throwable throwable = new RuntimeException((String) null);

        userNotifier.notify("Test message", throwable);

        String logs = getAllLogs();
        assertTrue(logs.contains("Well, this is embarrassing"));
        assertTrue(logs.contains("Test message"));
        assertTrue(logs.contains("RuntimeException"));
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
            logAppender.list.clear();
            userNotifier.notify("Test", exception);

            String logs = getAllLogs();
            assertTrue(logs.contains(exception.getClass().getSimpleName()));
        }
    }

    @Test
    void testNotify_ContainsContinueMessage() {
        userNotifier.notify("Test", new RuntimeException("Test"));

        String logs = getAllLogs();
        assertTrue(logs.contains("The application will continue running"));
    }

    @Test
    void testNotifyWithReport_ContainsPrivacyMessage() {
        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String logs = getAllLogs();
        assertTrue(logs.contains("We take privacy seriously"));
        assertTrue(logs.contains("do not perform any automated error collection"));
    }

    @Test
    void testNotifyWithReport_ContainsThankYouMessage() {
        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String logs = getAllLogs();
        assertTrue(logs.contains("Thank you kindly!"));
    }

    @Test
    void testNotifyWithReport_ContainsCrashReportSubject() {
        userNotifier.notifyWithReport("Test", "/path/to/report.json");

        String logs = getAllLogs();
        assertTrue(logs.contains("Test App Crash Report"));
    }

    @Test
    void testNotify_WithEmptyAppName() {
        when(metadata.getName()).thenReturn("");

        userNotifier.notify("Test", new RuntimeException("Test"));

        String logs = getAllLogs();
        assertTrue(logs.contains("encountered an error"));
    }

    @Test
    void testNotifyWithReport_WithNullMessage() {
        userNotifier.notifyWithReport(null, "/path/to/report.json");

        String logs = getAllLogs();
        assertTrue(logs.contains("Well, this is embarrassing"));
    }

    @Test
    void testNotifyWithReport_CompleteScenario() {
        userNotifier.notifyWithReport("Database connection failed", "/var/logs/crash-2024-01-01-12-00-00.json");

        String logs = getAllLogs();
        assertTrue(logs.contains("Well, this is embarrassing"));
        assertTrue(logs.contains("Test App"));
        assertTrue(logs.contains("Database connection failed"));
        assertTrue(logs.contains("/var/logs/crash-2024-01-01-12-00-00.json"));
        assertTrue(logs.contains("Test App Crash Report"));
        assertTrue(logs.contains("https://test.com/issues"));
        assertTrue(logs.contains("https://test.com/support"));
        assertTrue(logs.contains("Test Author"));
        assertTrue(logs.contains("Thank you kindly!"));
    }
}
