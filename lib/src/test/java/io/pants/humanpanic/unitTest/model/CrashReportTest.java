package io.pants.humanpanic.unitTest.model;

import io.pants.humanpanic.model.CrashReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CrashReport model classes
 */
class CrashReportTest {

    private CrashReport crashReport;

    @BeforeEach
    void setUp() {
        crashReport = new CrashReport();
    }

    @Test
    void testSetAndGetName() {
        crashReport.setName("Test Application");
        assertEquals("Test Application", crashReport.getName());
    }

    @Test
    void testSetAndGetOperatingSystem() {
        crashReport.setOperatingSystem("Linux 5.15.0");
        assertEquals("Linux 5.15.0", crashReport.getOperatingSystem());
    }

    @Test
    void testSetAndGetCrateVersion() {
        crashReport.setVersion("1.0.0");
        assertEquals("1.0.0", crashReport.getVersion());
    }

    @Test
    void testSetAndGetExplanation() {
        String explanation = "Test explanation";
        crashReport.setExplanation(explanation);
        assertEquals(explanation, crashReport.getExplanation());
    }

    @Test
    void testSetAndGetCause() {
        String cause = "NullPointerException: Test";
        crashReport.setCause(cause);
        assertEquals(cause, crashReport.getCause());
    }

    @Test
    void testSetAndGetMethod() {
        CrashReport.MethodInfo methodInfo = new CrashReport.MethodInfo();
        methodInfo.setClassName("com.example.Test");
        methodInfo.setMethodName("testMethod");

        crashReport.setMethod(methodInfo);

        assertNotNull(crashReport.getMethod());
        assertEquals("com.example.Test", crashReport.getMethod().getClassName());
        assertEquals("testMethod", crashReport.getMethod().getMethodName());
    }

    @Test
    void testSetAndGetBacktrace() {
        List<CrashReport.StackFrame> backtrace = new ArrayList<>();
        CrashReport.StackFrame frame = new CrashReport.StackFrame();
        frame.setClassName("com.example.Test");
        frame.setMethod("testMethod");
        frame.setFile("Test.java");
        frame.setLine(42);
        backtrace.add(frame);

        crashReport.setBacktrace(backtrace);

        assertNotNull(crashReport.getBacktrace());
        assertEquals(1, crashReport.getBacktrace().size());
        assertEquals("com.example.Test", crashReport.getBacktrace().get(0).getClassName());
    }

    @Test
    void testSetAndGetSystemInfo() {
        CrashReport.SystemInfo systemInfo = new CrashReport.SystemInfo();
        systemInfo.setJavaVersion("17.0.1");
        systemInfo.setOsName("Linux");
        systemInfo.setProcessors(8);

        crashReport.setSystemInfo(systemInfo);

        assertNotNull(crashReport.getSystemInfo());
        assertEquals("17.0.1", crashReport.getSystemInfo().getJavaVersion());
        assertEquals("Linux", crashReport.getSystemInfo().getOsName());
        assertEquals(8, crashReport.getSystemInfo().getProcessors());
    }

    @Test
    void testSetAndGetApplicationInfo() {
        Map<String, String> appInfo = new HashMap<>();
        appInfo.put("name", "Test App");
        appInfo.put("version", "1.0.0");

        crashReport.setApplicationInfo(appInfo);

        assertNotNull(crashReport.getApplicationInfo());
        assertEquals("Test App", crashReport.getApplicationInfo().get("name"));
        assertEquals("1.0.0", crashReport.getApplicationInfo().get("version"));
    }

    @Test
    void testMethodInfo_SetAndGetClassName() {
        CrashReport.MethodInfo methodInfo = new CrashReport.MethodInfo();
        methodInfo.setClassName("com.example.TestClass");

        assertEquals("com.example.TestClass", methodInfo.getClassName());
    }

    @Test
    void testMethodInfo_SetAndGetMethodName() {
        CrashReport.MethodInfo methodInfo = new CrashReport.MethodInfo();
        methodInfo.setMethodName("testMethod");

        assertEquals("testMethod", methodInfo.getMethodName());
    }

    @Test
    void testStackFrame_AllFields() {
        CrashReport.StackFrame frame = new CrashReport.StackFrame();
        frame.setClassName("com.example.Test");
        frame.setMethod("testMethod");
        frame.setFile("Test.java");
        frame.setLine(100);

        assertEquals("com.example.Test", frame.getClassName());
        assertEquals("testMethod", frame.getMethod());
        assertEquals("Test.java", frame.getFile());
        assertEquals(100, frame.getLine());
    }

    @Test
    void testStackFrame_WithNullLine() {
        CrashReport.StackFrame frame = new CrashReport.StackFrame();
        frame.setLine(null);

        assertNull(frame.getLine());
    }

    @Test
    void testSystemInfo_AllFields() {
        CrashReport.SystemInfo systemInfo = new CrashReport.SystemInfo();
        systemInfo.setJavaVersion("17.0.1");
        systemInfo.setJavaVendor("Oracle");
        systemInfo.setOsName("Linux");
        systemInfo.setOsVersion("5.15.0");
        systemInfo.setOsArch("amd64");
        systemInfo.setMaxMemoryMb(2048);
        systemInfo.setTotalMemoryMb(1024);
        systemInfo.setFreeMemoryMb(512);
        systemInfo.setProcessors(8);
        systemInfo.setUptimeMs(60000);

        assertEquals("17.0.1", systemInfo.getJavaVersion());
        assertEquals("Oracle", systemInfo.getJavaVendor());
        assertEquals("Linux", systemInfo.getOsName());
        assertEquals("5.15.0", systemInfo.getOsVersion());
        assertEquals("amd64", systemInfo.getOsArch());
        assertEquals(2048, systemInfo.getMaxMemoryMb());
        assertEquals(1024, systemInfo.getTotalMemoryMb());
        assertEquals(512, systemInfo.getFreeMemoryMb());
        assertEquals(8, systemInfo.getProcessors());
        assertEquals(60000, systemInfo.getUptimeMs());
    }

    @Test
    void testCompleteCrashReport() {
        // Create complete crash report
        crashReport.setName("Test App");
        crashReport.setOperatingSystem("Linux 5.15.0");
        crashReport.setVersion("1.0.0");
        crashReport.setExplanation("Test explanation");
        crashReport.setCause("RuntimeException: Test error");

        // Method info
        CrashReport.MethodInfo methodInfo = new CrashReport.MethodInfo();
        methodInfo.setClassName("com.example.Service");
        methodInfo.setMethodName("processData");
        crashReport.setMethod(methodInfo);

        // Backtrace
        List<CrashReport.StackFrame> backtrace = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            CrashReport.StackFrame frame = new CrashReport.StackFrame();
            frame.setClassName("com.example.Class" + i);
            frame.setMethod("method" + i);
            frame.setFile("Class" + i + ".java");
            frame.setLine(10 + i);
            backtrace.add(frame);
        }
        crashReport.setBacktrace(backtrace);

        // System info
        CrashReport.SystemInfo systemInfo = new CrashReport.SystemInfo();
        systemInfo.setJavaVersion("17.0.1");
        systemInfo.setJavaVendor("Oracle");
        systemInfo.setOsName("Linux");
        systemInfo.setOsVersion("5.15.0");
        systemInfo.setOsArch("amd64");
        systemInfo.setMaxMemoryMb(2048);
        systemInfo.setTotalMemoryMb(1024);
        systemInfo.setFreeMemoryMb(512);
        systemInfo.setProcessors(8);
        systemInfo.setUptimeMs(60000);
        crashReport.setSystemInfo(systemInfo);

        // Application info
        Map<String, String> appInfo = new HashMap<>();
        appInfo.put("name", "Test App");
        appInfo.put("version", "1.0.0");
        appInfo.put("authors", "Author1, Author2");
        crashReport.setApplicationInfo(appInfo);

        // Verify all fields
        assertEquals("Test App", crashReport.getName());
        assertEquals("Linux 5.15.0", crashReport.getOperatingSystem());
        assertEquals("1.0.0", crashReport.getVersion());
        assertNotNull(crashReport.getExplanation());
        assertNotNull(crashReport.getCause());
        assertNotNull(crashReport.getMethod());
        assertEquals(3, crashReport.getBacktrace().size());
        assertNotNull(crashReport.getSystemInfo());
        assertNotNull(crashReport.getApplicationInfo());
    }

    @Test
    void testNullValues() {
        crashReport.setName(null);
        crashReport.setOperatingSystem(null);
        crashReport.setVersion(null);
        crashReport.setExplanation(null);
        crashReport.setCause(null);
        crashReport.setMethod(null);
        crashReport.setBacktrace(null);
        crashReport.setSystemInfo(null);
        crashReport.setApplicationInfo(null);

        assertNull(crashReport.getName());
        assertNull(crashReport.getOperatingSystem());
        assertNull(crashReport.getVersion());
        assertNull(crashReport.getExplanation());
        assertNull(crashReport.getCause());
        assertNull(crashReport.getMethod());
        assertNull(crashReport.getBacktrace());
        assertNull(crashReport.getSystemInfo());
        assertNull(crashReport.getApplicationInfo());
    }

    @Test
    void testEmptyBacktrace() {
        crashReport.setBacktrace(new ArrayList<>());

        assertNotNull(crashReport.getBacktrace());
        assertEquals(0, crashReport.getBacktrace().size());
    }

    @Test
    void testEmptyApplicationInfo() {
        crashReport.setApplicationInfo(new HashMap<>());

        assertNotNull(crashReport.getApplicationInfo());
        assertEquals(0, crashReport.getApplicationInfo().size());
    }

    @Test
    void testStackFrame_WithNegativeLine() {
        CrashReport.StackFrame frame = new CrashReport.StackFrame();
        frame.setLine(-1);

        assertEquals(-1, frame.getLine());
    }

    @Test
    void testSystemInfo_WithZeroValues() {
        CrashReport.SystemInfo systemInfo = new CrashReport.SystemInfo();
        systemInfo.setMaxMemoryMb(0);
        systemInfo.setTotalMemoryMb(0);
        systemInfo.setFreeMemoryMb(0);
        systemInfo.setProcessors(0);
        systemInfo.setUptimeMs(0);

        assertEquals(0, systemInfo.getMaxMemoryMb());
        assertEquals(0, systemInfo.getTotalMemoryMb());
        assertEquals(0, systemInfo.getFreeMemoryMb());
        assertEquals(0, systemInfo.getProcessors());
        assertEquals(0, systemInfo.getUptimeMs());
    }

    @Test
    void testLargeBacktrace() {
        List<CrashReport.StackFrame> backtrace = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            CrashReport.StackFrame frame = new CrashReport.StackFrame();
            frame.setClassName("Class" + i);
            frame.setMethod("method" + i);
            backtrace.add(frame);
        }

        crashReport.setBacktrace(backtrace);

        assertEquals(100, crashReport.getBacktrace().size());
    }

    @Test
    void testApplicationInfo_MultipleEntries() {
        Map<String, String> appInfo = new HashMap<>();
        appInfo.put("name", "Test App");
        appInfo.put("version", "1.0.0");
        appInfo.put("authors", "Author1, Author2");
        appInfo.put("homepage", "https://test.com");
        appInfo.put("support", "https://test.com/support");
        appInfo.put("issues", "https://test.com/issues");

        crashReport.setApplicationInfo(appInfo);

        assertEquals(6, crashReport.getApplicationInfo().size());
        assertTrue(crashReport.getApplicationInfo().containsKey("homepage"));
        assertTrue(crashReport.getApplicationInfo().containsKey("support"));
        assertTrue(crashReport.getApplicationInfo().containsKey("issues"));
    }
}
