package io.pants.humanpanic.e2eTest;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.config.HumanPanicConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End tests for HumanPanic - No mocking, real annotation processing
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HumanPanicConfiguration.class)
@TestPropertySource(properties = {
        "app.name=E2E Test Application",
        "app.version=2.0.0-e2e",
        "app.authors=John Doe, Jane Smith, Bob Wilson",
        "app.homepage=https://e2etest.example.com",
        "app.support-url=https://support.e2etest.example.com",
        "app.issue-url=https://github.com/e2etest/issues"
})
class HumanPanicE2ETest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TestService testService;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger userNotifierLogger;
    private static final String CRASH_REPORTS_DIR = "crash-reports";

    @BeforeEach
    void setUp() {
        // Setup log capturing
        userNotifierLogger = (Logger) LoggerFactory.getLogger("io.pants.humanpanic.reporter.UserNotifier");
        logAppender = new ListAppender<>();
        logAppender.start();
        userNotifierLogger.addAppender(logAppender);
        logAppender.list.clear();
        assertTrue(logAppender.list.isEmpty(), "Log appender should be empty before test");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up log appender
        if (logAppender != null) {
            userNotifierLogger.detachAppender(logAppender);
            logAppender.stop();
        }

        // Clean up crash reports
        cleanupCrashReports();
    }

    @Test
    void testDefaultConfiguration_CreatesCrashReportAndLogs() {
        // Execute method with default @HumanPanic annotation
        testService.methodWithDefaultConfig();

        // Verify crash report was created
        File crashReportsDir = new File(CRASH_REPORTS_DIR);
        assertTrue(crashReportsDir.exists(), "Crash reports directory should exist");

        File[] reports = crashReportsDir.listFiles((dir, name) -> name.endsWith(".json"));
        assertNotNull(reports);
        assertEquals(1, reports.length, "Should have created exactly one crash report");

        // Verify report content
        String reportContent = readFile(reports[0].toPath());
        assertReportContainsExpectedData(reportContent, "methodWithDefaultConfig");

        // Verify logs were created
        List<ILoggingEvent> logEvents = logAppender.list;
        assertFalse(logEvents.isEmpty(), "Should have logged error messages");

        String allLogs = getAllLogs(logEvents);
        assertTrue(allLogs.contains("Well, this is embarrassing"), "Should contain embarrassing message");
        assertTrue(allLogs.contains("E2E Test Application"), "Should contain app name");
        assertTrue(allLogs.contains(reports[0].getAbsolutePath()), "Should contain report path");
    }

    @Test
    void testCustomMessage_AppearsInLogs() {
        testService.methodWithCustomMessage();

        List<ILoggingEvent> logEvents = logAppender.list;
        String allLogs = getAllLogs(logEvents);

        assertTrue(allLogs.contains("Database connection failed"), "Should contain custom message");
        assertTrue(allLogs.contains("E2E Test Application"), "Should contain app name");
    }

    @Test
    void testNoCrashReport_OnlyLogsError() {
        testService.methodWithoutCrashReport();

        // Verify no crash report was created
        File crashReportsDir = new File(CRASH_REPORTS_DIR);
        if (crashReportsDir.exists()) {
            File[] reports = crashReportsDir.listFiles((dir, name) -> name.endsWith(".json"));
            assertEquals(0, reports != null ? reports.length : 0, "Should not create crash report");
        }

        // Verify error was logged
        List<ILoggingEvent> logEvents = logAppender.list;
        assertFalse(logEvents.isEmpty(), "Should have logged error messages");

        String allLogs = getAllLogs(logEvents);
        assertTrue(allLogs.contains("Well, this is embarrassing"), "Should contain error message");
        assertTrue(allLogs.contains("RuntimeException"), "Should mention exception type");
    }

    @Test
    void testSilentMode_NoLogsNoOutput() {
        testService.methodInSilentMode();

        // Verify crash report was created (silent only suppresses user notification)
        File crashReportsDir = new File(CRASH_REPORTS_DIR);
        File[] reports = crashReportsDir.listFiles((dir, name) -> name.endsWith(".json"));
        assertEquals(1, reports != null ? reports.length : 0, "Should still create crash report in silent mode");

        // Verify no user notification logs (only internal logs might exist)
        List<ILoggingEvent> logEvents = logAppender.list;
        String allLogs = getAllLogs(logEvents);

        // Should not contain user-facing messages
        assertFalse(allLogs.contains("Well, this is embarrassing"), "Should not log user-facing messages in silent mode");
    }

    @Test
    void testMethodReturnsDefaultValue_OnException() {
        // Test with different return types
        int resultInt = testService.methodReturningInt();
        assertEquals(0, resultInt, "Should return default int value (0)");

        boolean resultBoolean = testService.methodReturningBoolean();
        assertFalse(resultBoolean, "Should return default boolean value (false)");

        String resultString = testService.methodReturningString();
        assertNull(resultString, "Should return default object value (null)");

        long resultLong = testService.methodReturningLong();
        assertEquals(0L, resultLong, "Should return default long value (0L)");

        double resultDouble = testService.methodReturningDouble();
        assertEquals(0.0d, resultDouble, "Should return default double value (0.0)");
    }

    @Test
    void testCrashReportContainsAllMetadata() {
        testService.methodWithDefaultConfig();

        File crashReportsDir = new File(CRASH_REPORTS_DIR);
        File[] reports = crashReportsDir.listFiles((dir, name) -> name.endsWith(".json"));
        assertNotNull(reports);
        assertEquals(1, reports.length);

        String reportContent = readFile(reports[0].toPath());

        // Verify all application metadata
        assertTrue(reportContent.contains("E2E Test Application"), "Should contain app name");
        assertTrue(reportContent.contains("2.0.0-e2e"), "Should contain app version");
        assertTrue(reportContent.contains("John Doe"), "Should contain author 1");
        assertTrue(reportContent.contains("Jane Smith"), "Should contain author 2");
        assertTrue(reportContent.contains("Bob Wilson"), "Should contain author 3");
        assertTrue(reportContent.contains("https://e2etest.example.com"), "Should contain homepage");
        assertTrue(reportContent.contains("https://support.e2etest.example.com"), "Should contain support URL");
        assertTrue(reportContent.contains("https://github.com/e2etest/issues"), "Should contain issue URL");

        // Verify system information
        assertTrue(reportContent.contains("system_info"), "Should contain system info");
        assertTrue(reportContent.contains("java_version"), "Should contain Java version");
        assertTrue(reportContent.contains("os_name"), "Should contain OS name");
        assertTrue(reportContent.contains("processors"), "Should contain processor count");

        // Verify backtrace
        assertTrue(reportContent.contains("backtrace"), "Should contain backtrace");
        assertTrue(reportContent.contains("methodWithDefaultConfig"), "Should contain method name in backtrace");
    }

    @Test
    void testLogsContainAllRequiredInformation() {
        testService.methodWithCustomMessage();

        List<ILoggingEvent> logEvents = logAppender.list;
        String allLogs = getAllLogs(logEvents);

        // Verify all required log components
        assertTrue(allLogs.contains("Well, this is embarrassing"), "Should have opening message");
        assertTrue(allLogs.contains("E2E Test Application"), "Should mention app name");
        assertTrue(allLogs.contains("had a problem and crashed"), "Should describe the problem");
        assertTrue(allLogs.contains("Database connection failed"), "Should contain custom message");
        assertTrue(allLogs.contains("We have generated a report file at"), "Should mention report generation");
        assertTrue(allLogs.contains("Submit an issue or email"), "Should provide submission instructions");
        assertTrue(allLogs.contains("E2E Test Application Crash Report"), "Should provide report subject");
        assertTrue(allLogs.contains("https://github.com/e2etest/issues"), "Should contain issue URL");
        assertTrue(allLogs.contains("https://support.e2etest.example.com"), "Should contain support URL");
        assertTrue(allLogs.contains("John Doe"), "Should contain author name");
        assertTrue(allLogs.contains("We take privacy seriously"), "Should contain privacy statement");
        assertTrue(allLogs.contains("Thank you kindly"), "Should have closing message");
    }

    @Test
    void testMultipleCrashReports_UniqueFilenames() throws InterruptedException {
        // Create multiple crash reports
        testService.methodWithDefaultConfig();
        Thread.sleep(1100); // Ensure different timestamp
        testService.methodWithCustomMessage();
        Thread.sleep(1100);
        testService.methodReturningString();

        File crashReportsDir = new File(CRASH_REPORTS_DIR);
        File[] reports = crashReportsDir.listFiles((dir, name) -> name.endsWith(".json"));

        assertNotNull(reports);
        assertEquals(3, reports.length, "Should create 3 unique crash reports");

        // Verify all filenames are unique
        List<String> filenames = List.of(reports).stream()
                .map(File::getName)
                .collect(Collectors.toList());

        long uniqueCount = filenames.stream().distinct().count();
        assertEquals(3, uniqueCount, "All report filenames should be unique");
    }

    @Test
    void testDifferentExceptionTypes() throws IOException {
        // Test NullPointerException
        testService.methodThrowingNPE();
        File[] reports1 = getCrashReports();
        assertEquals(1, reports1.length);
        String content1 = readFile(reports1[0].toPath());
        assertTrue(content1.contains("NullPointerException"), "Should capture NPE");
        cleanupCrashReports();

        // Test IllegalArgumentException
        testService.methodThrowingIAE();
        File[] reports2 = getCrashReports();
        assertEquals(1, reports2.length);
        String content2 = readFile(reports2[0].toPath());
        assertTrue(content2.contains("IllegalArgumentException"), "Should capture IAE");
        cleanupCrashReports();

        // Test IllegalStateException
        testService.methodThrowingISE();
        File[] reports3 = getCrashReports();
        assertEquals(1, reports3.length);
        String content3 = readFile(reports3[0].toPath());
        assertTrue(content3.contains("IllegalStateException"), "Should capture ISE");
    }

    @Test
    void testEmptyMessageUsesDefault() {
        testService.methodWithEmptyMessage();

        List<ILoggingEvent> logEvents = logAppender.list;
        String allLogs = getAllLogs(logEvents);

        // Should use default "An error occurred" message
        assertTrue(allLogs.contains("Well, this is embarrassing"), "Should have default opening");
        // The "An error occurred" message should NOT appear as custom message
        assertFalse(allLogs.contains("An error occurred\n"), "Should not show default message separately");
    }

    @Test
    void testVoidMethod_HandlesExceptionCorrectly() {
        // Should not throw exception to caller
        assertDoesNotThrow(() -> testService.voidMethodWithError());

        // Verify crash report was created
        File[] reports = getCrashReports();
        assertEquals(1, reports.length, "Should create crash report for void method");
    }

    @Test
    void testMethodWithArguments_CapturedInReport() {
        testService.methodWithArguments("testParam", 42);

        File[] reports = getCrashReports();
        assertEquals(1, reports.length);
        String content = readFile(reports[0].toPath());

        // Verify method name is captured
        assertTrue(content.contains("methodWithArguments"), "Should contain method name");
    }

    @Test
    void testCrashReportJSON_ValidStructure() {
        testService.methodWithDefaultConfig();

        File[] reports = getCrashReports();
        String content = readFile(reports[0].toPath());

        // Verify JSON structure
        assertTrue(content.contains("\"name\""), "Should have name field");
        assertTrue(content.contains("\"operating_system\""), "Should have OS field");
        assertTrue(content.contains("\"version\""), "Should have version field");
        assertTrue(content.contains("\"explanation\""), "Should have explanation field");
        assertTrue(content.contains("\"cause\""), "Should have cause field");
        assertTrue(content.contains("\"method\""), "Should have method field");
        assertTrue(content.contains("\"backtrace\""), "Should have backtrace field");
        assertTrue(content.contains("\"system_info\""), "Should have system_info field");
        assertTrue(content.contains("\"application_info\""), "Should have application_info field");
    }

    // Helper methods

    private void assertReportContainsExpectedData(String reportContent, String methodName) {
        assertTrue(reportContent.contains("E2E Test Application"), "Should contain app name");
        assertTrue(reportContent.contains("2.0.0-e2e"), "Should contain version");
        assertTrue(reportContent.contains(methodName), "Should contain method name");
        assertTrue(reportContent.contains("RuntimeException"), "Should contain exception type");
    }

    private String getAllLogs(List<ILoggingEvent> logEvents) {
        return logEvents.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            fail("Failed to read file: " + path);
            return "";
        }
    }

    private File[] getCrashReports() {
        File crashReportsDir = new File(CRASH_REPORTS_DIR);
        if (!crashReportsDir.exists()) {
            return new File[0];
        }
        File[] reports = crashReportsDir.listFiles((dir, name) -> name.endsWith(".json"));
        return reports != null ? reports : new File[0];
    }

    private void cleanupCrashReports() throws IOException {
        Path crashReportsPath = Paths.get(CRASH_REPORTS_DIR);
        if (Files.exists(crashReportsPath)) {
            Files.walk(crashReportsPath)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * Test service with various @HumanPanic configurations
     */
    @Service
    public static class TestService {

        @HumanPanic
        public void methodWithDefaultConfig() {
            throw new RuntimeException("Default config test exception");
        }

        @HumanPanic(message = "Database connection failed")
        public void methodWithCustomMessage() {
            throw new RuntimeException("DB connection error");
        }

        @HumanPanic(createCrashReport = false)
        public void methodWithoutCrashReport() {
            throw new RuntimeException("No crash report exception");
        }

        @HumanPanic(silent = true)
        public void methodInSilentMode() {
            throw new RuntimeException("Silent exception");
        }

        @HumanPanic
        public int methodReturningInt() {
            throw new RuntimeException("Int method exception");
        }

        @HumanPanic
        public boolean methodReturningBoolean() {
            throw new RuntimeException("Boolean method exception");
        }

        @HumanPanic
        public String methodReturningString() {
            throw new RuntimeException("String method exception");
        }

        @HumanPanic
        public long methodReturningLong() {
            throw new RuntimeException("Long method exception");
        }

        @HumanPanic
        public double methodReturningDouble() {
            throw new RuntimeException("Double method exception");
        }

        @HumanPanic
        public void methodThrowingNPE() {
            throw new NullPointerException("Null pointer test");
        }

        @HumanPanic
        public void methodThrowingIAE() {
            throw new IllegalArgumentException("Illegal argument test");
        }

        @HumanPanic
        public void methodThrowingISE() {
            throw new IllegalStateException("Illegal state test");
        }

        @HumanPanic(message = "")
        public void methodWithEmptyMessage() {
            throw new RuntimeException("Empty message exception");
        }

        @HumanPanic
        public void voidMethodWithError() {
            throw new RuntimeException("Void method exception");
        }

        @HumanPanic
        public String methodWithArguments(String param1, int param2) {
            throw new RuntimeException("Method with args exception");
        }
    }
}
