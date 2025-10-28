package io.pants.humanpanic.unitTest.reporter;

import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.ConfigLoader;
import io.pants.humanpanic.reporter.CrashReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CrashReporter
 */
class CrashReporterTest {

    @Mock
    private ConfigLoader configLoader;

    @Mock
    private AppMetadata metadata;

    private CrashReporter crashReporter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(configLoader.getMetadata()).thenReturn(metadata);
        when(metadata.getName()).thenReturn("Test App");
        when(metadata.getVersion()).thenReturn("1.0.0");
        when(metadata.getAuthors()).thenReturn(new String[]{"Test Author"});
        when(metadata.getHomepage()).thenReturn("https://test.com");
        when(metadata.getSupportUrl()).thenReturn("https://test.com/support");
        when(metadata.getIssueUrl()).thenReturn("https://test.com/issues");

        crashReporter = new CrashReporter(configLoader);
    }

    @Test
    void testCreateReport_Success() throws Exception {
        Method testMethod = getClass().getDeclaredMethod("testCreateReport_Success");
        Throwable throwable = new RuntimeException("Test exception");

        String reportPath = crashReporter.createReport(throwable, testMethod);

        assertNotNull(reportPath);
        assertTrue(reportPath.contains("crash-reports"));
        assertTrue(reportPath.endsWith(".json"));

        File reportFile = new File(reportPath);
        assertTrue(reportFile.exists());
        assertTrue(reportFile.length() > 0);
    }

    @Test
    void testCreateReport_WithNullMessage() throws Exception {
        Method testMethod = getClass().getDeclaredMethod("testCreateReport_WithNullMessage");
        Throwable throwable = new RuntimeException((String) null);

        String reportPath = crashReporter.createReport(throwable, testMethod);

        assertNotNull(reportPath);
        File reportFile = new File(reportPath);
        assertTrue(reportFile.exists());
    }

    @Test
    void testCreateReport_WithStackTrace() throws Exception {
        Method testMethod = getClass().getDeclaredMethod("testCreateReport_WithStackTrace");
        Throwable throwable = createThrowableWithStackTrace();

        String reportPath = crashReporter.createReport(throwable, testMethod);

        assertNotNull(reportPath);
        File reportFile = new File(reportPath);
        assertTrue(reportFile.exists());

        String content = Files.readString(reportFile.toPath());
        assertTrue(content.contains("backtrace"));
        assertTrue(content.contains("class"));
        assertTrue(content.contains("method"));
    }

    @Test
    void testCreateReport_WithNullMethod() {
        Throwable throwable = new RuntimeException("Test exception");

        String reportPath = crashReporter.createReport(throwable, null);

        assertNotNull(reportPath);
        File reportFile = new File(reportPath);
        assertTrue(reportFile.exists());
    }

    @Test
    void testCreateReport_ContainsSystemInfo() throws Exception {
        Method testMethod = getClass().getDeclaredMethod("testCreateReport_ContainsSystemInfo");
        Throwable throwable = new RuntimeException("Test exception");

        String reportPath = crashReporter.createReport(throwable, testMethod);
        File reportFile = new File(reportPath);
        String content = Files.readString(reportFile.toPath());

        assertTrue(content.contains("system_info"));
        assertTrue(content.contains("java_version"));
        assertTrue(content.contains("os_name"));
        assertTrue(content.contains("processors"));
    }

    @Test
    void testCreateReport_ContainsApplicationInfo() throws Exception {
        Method testMethod = getClass().getDeclaredMethod("testCreateReport_ContainsApplicationInfo");
        Throwable throwable = new RuntimeException("Test exception");

        String reportPath = crashReporter.createReport(throwable, testMethod);
        File reportFile = new File(reportPath);
        String content = Files.readString(reportFile.toPath());

        assertTrue(content.contains("application_info"));
        assertTrue(content.contains("Test App"));
        assertTrue(content.contains("1.0.0"));
        assertTrue(content.contains("Test Author"));
    }

    @Test
    void testCreateReport_DifferentExceptionTypes() throws Exception {
        Method testMethod = getClass().getDeclaredMethod("testCreateReport_DifferentExceptionTypes");

        // Test with different exception types
        Exception[] exceptions = {
                new NullPointerException("NPE test"),
                new IllegalArgumentException("IAE test"),
                new IllegalStateException("ISE test"),
                new IndexOutOfBoundsException("IOOBE test")
        };

        for (Exception exception : exceptions) {
            String reportPath = crashReporter.createReport(exception, testMethod);
            assertNotNull(reportPath);
            File reportFile = new File(reportPath);
            assertTrue(reportFile.exists());

            String content = Files.readString(reportFile.toPath());
            assertTrue(content.contains(exception.getClass().getName()));
        }
    }

    @Test
    void testCreateReport_CreatesDirectory() {
        Method testMethod = null;
        try {
            testMethod = getClass().getDeclaredMethod("testCreateReport_CreatesDirectory");
        } catch (NoSuchMethodException e) {
            fail("Method not found");
        }

        Throwable throwable = new RuntimeException("Test exception");

        String reportPath = crashReporter.createReport(throwable, testMethod);

        assertNotNull(reportPath);
        File reportDir = new File("crash-reports");
        assertTrue(reportDir.exists());
        assertTrue(reportDir.isDirectory());
    }

    @Test
    void testCreateReport_UniqueFilenames() throws Exception {
        Method testMethod = getClass().getDeclaredMethod("testCreateReport_UniqueFilenames");
        Throwable throwable = new RuntimeException("Test exception");

        String reportPath1 = crashReporter.createReport(throwable, testMethod);
        Thread.sleep(1000); // Ensure different timestamp
        String reportPath2 = crashReporter.createReport(throwable, testMethod);

        assertNotEquals(reportPath1, reportPath2);
    }

    @Test
    void testCreateReport_WithEmptyMetadata() throws Exception {
        when(metadata.getName()).thenReturn("");
        when(metadata.getVersion()).thenReturn("");
        when(metadata.getAuthors()).thenReturn(new String[]{});
        when(metadata.getHomepage()).thenReturn("");
        when(metadata.getSupportUrl()).thenReturn("");
        when(metadata.getIssueUrl()).thenReturn("");

        Method testMethod = getClass().getDeclaredMethod("testCreateReport_WithEmptyMetadata");
        Throwable throwable = new RuntimeException("Test exception");

        String reportPath = crashReporter.createReport(throwable, testMethod);

        assertNotNull(reportPath);
        File reportFile = new File(reportPath);
        assertTrue(reportFile.exists());
    }

    @Test
    void testCreateReport_WithLongStackTrace() throws Exception {
        Method testMethod = getClass().getDeclaredMethod("testCreateReport_WithLongStackTrace");
        Throwable throwable = createDeepStackTrace();

        String reportPath = crashReporter.createReport(throwable, testMethod);

        assertNotNull(reportPath);
        File reportFile = new File(reportPath);
        assertTrue(reportFile.exists());

        String content = Files.readString(reportFile.toPath());
        assertTrue(content.length() > 1000); // Should have substantial content
    }

    // Helper methods
    private Throwable createThrowableWithStackTrace() {
        try {
            methodA();
        } catch (Exception e) {
            return e;
        }
        return new RuntimeException("Fallback");
    }

    private void methodA() {
        methodB();
    }

    private void methodB() {
        methodC();
    }

    private void methodC() {
        throw new RuntimeException("Deep stack trace");
    }

    private Throwable createDeepStackTrace() {
        return createThrowableWithStackTrace();
    }
}
