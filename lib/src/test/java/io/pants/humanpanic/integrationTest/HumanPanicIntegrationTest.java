package io.pants.humanpanic.integrationTest;

import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.ConfigLoader;
import io.pants.humanpanic.config.HumanPanicConfiguration;
import io.pants.humanpanic.interceptor.HumanPanicAspect;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for HumanPanic library with Spring context
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HumanPanicConfiguration.class)
@TestPropertySource(properties = {
        "app.name=Integration Test App",
        "app.version=1.0.0-test",
        "app.authors=Test Author1, Test Author2",
        "app.homepage=https://test.example.com",
        "app.support-url=https://support.test.example.com",
        "app.issue-url=https://github.com/test/issues"
})
class HumanPanicIntegrationTest {

    @Autowired
    private AppMetadata appMetadata;

    @Autowired
    private ConfigLoader configLoader;

    @Autowired
    private CrashReporter crashReporter;

    @Autowired
    private UserNotifier userNotifier;

    @Autowired
    private HumanPanicAspect humanPanicAspect;

    @AfterEach
    void tearDown() {
        // Clean up generated reports
        File reportDir = new File("crash-reports");
        if (reportDir.exists() && reportDir.isDirectory()) {
            for (File file : reportDir.listFiles()) {
                file.delete();
            }
            reportDir.delete();
        }
    }

    @Test
    void testSpringContextLoads() {
        assertNotNull(appMetadata);
        assertNotNull(configLoader);
        assertNotNull(crashReporter);
        assertNotNull(userNotifier);
        assertNotNull(humanPanicAspect);
    }

    @Test
    void testConfigurationIsLoaded() {
        assertEquals("Integration Test App", appMetadata.getName());
        assertEquals("1.0.0-test", appMetadata.getVersion());
        assertEquals(2, appMetadata.getAuthors().length);
        assertEquals("Test Author1", appMetadata.getAuthors()[0]);
        assertEquals("Test Author2", appMetadata.getAuthors()[1]);
        assertEquals("https://test.example.com", appMetadata.getHomepage());
        assertEquals("https://support.test.example.com", appMetadata.getSupportUrl());
        assertEquals("https://github.com/test/issues", appMetadata.getIssueUrl());
    }

    @Test
    void testCrashReporterCreatesReport() throws Exception {
        RuntimeException exception = new RuntimeException("Integration test exception");
        java.lang.reflect.Method method = TestService.class.getMethod("testMethod");

        String reportPath = crashReporter.createReport(exception, method);

        assertNotNull(reportPath);
        assertTrue(reportPath.contains("crash-reports"));
        assertTrue(reportPath.endsWith(".json"));

        File reportFile = new File(reportPath);
        assertTrue(reportFile.exists());
        assertTrue(reportFile.length() > 0);

        // Clean up
        reportFile.delete();
    }

    @Test
    void testUserNotifierWithReport() {
        // This will print to System.err, but we just verify no exceptions
        assertDoesNotThrow(() ->
                userNotifier.notifyWithReport("Test message", "/path/to/report.json")
        );
    }

    @Test
    void testUserNotifierWithoutReport() {
        RuntimeException exception = new RuntimeException("Test");

        assertDoesNotThrow(() ->
                userNotifier.notify("Test message", exception)
        );
    }

    @Test
    void testBeansAreSingleton() {
        // Verify beans are singleton scoped
        assertSame(appMetadata, configLoader.getMetadata());
    }

    @Test
    void testConfigLoaderInitialization() {
        AppMetadata metadata = configLoader.getMetadata();

        assertNotNull(metadata);
        assertNotNull(metadata.getName());
        assertNotNull(metadata.getVersion());
        assertNotNull(metadata.getAuthors());
    }

    @Test
    void testCrashReporterWithNullMethod() {
        RuntimeException exception = new RuntimeException("Test exception");

        String reportPath = crashReporter.createReport(exception, null);

        assertNotNull(reportPath);
        File reportFile = new File(reportPath);
        assertTrue(reportFile.exists());

        // Clean up
        reportFile.delete();
    }

    @Test
    void testCrashReporterWithDifferentExceptions() throws Exception {
        java.lang.reflect.Method method = TestService.class.getMethod("testMethod");

        Exception[] exceptions = {
                new NullPointerException("NPE test"),
                new IllegalArgumentException("IAE test"),
                new IllegalStateException("ISE test")
        };

        for (Exception exception : exceptions) {
            String reportPath = crashReporter.createReport(exception, method);
            assertNotNull(reportPath);

            File reportFile = new File(reportPath);
            assertTrue(reportFile.exists());

            // Clean up
            reportFile.delete();
        }
    }

    @Test
    void testApplicationInfoInCrashReport() throws Exception {
        RuntimeException exception = new RuntimeException("Test");
        java.lang.reflect.Method method = TestService.class.getMethod("testMethod");

        String reportPath = crashReporter.createReport(exception, method);
        assertNotNull(reportPath);

        File reportFile = new File(reportPath);
        String content = java.nio.file.Files.readString(reportFile.toPath());

        assertTrue(content.contains("Integration Test App"));
        assertTrue(content.contains("1.0.0-test"));
        assertTrue(content.contains("Test Author1"));
        assertTrue(content.contains("Test Author2"));

        // Clean up
        reportFile.delete();
    }

    @Test
    void testMultipleCrashReportsHaveUniqueNames() throws Exception {
        RuntimeException exception = new RuntimeException("Test");
        java.lang.reflect.Method method = TestService.class.getMethod("testMethod");

        String report1 = crashReporter.createReport(exception, method);
        Thread.sleep(1100); // Ensure different timestamp
        String report2 = crashReporter.createReport(exception, method);

        assertNotEquals(report1, report2);

        // Clean up
        new File(report1).delete();
        new File(report2).delete();
    }

    @Test
    void testCrashReportDirectoryCreation() throws Exception {
        // Delete crash-reports directory if it exists
        File crashDir = new File("crash-reports");
        if (crashDir.exists()) {
            for (File file : crashDir.listFiles()) {
                file.delete();
            }
            crashDir.delete();
        }

        RuntimeException exception = new RuntimeException("Test");
        java.lang.reflect.Method method = TestService.class.getMethod("testMethod");

        String reportPath = crashReporter.createReport(exception, method);

        assertTrue(crashDir.exists());
        assertTrue(crashDir.isDirectory());

        // Clean up
        new File(reportPath).delete();
    }

    // Test service class
    public static class TestService {
        @HumanPanic
        public void testMethod() {
            throw new RuntimeException("Test exception");
        }

        @HumanPanic(message = "Custom error")
        public void methodWithCustomMessage() {
            throw new RuntimeException("Test exception");
        }

        @HumanPanic(silent = true)
        public void silentMethod() {
            throw new RuntimeException("Test exception");
        }

        @HumanPanic(createCrashReport = false)
        public void noCrashReportMethod() {
            throw new RuntimeException("Test exception");
        }
    }
}
