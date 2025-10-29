package io.pants.humanpanic.reporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.ConfigLoader;
import io.pants.humanpanic.model.CrashReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Creates JSON crash reports similar to Rust's human-panic
 */
@Slf4j
@RequiredArgsConstructor
public class CrashReporter {

    private final ConfigLoader configLoader;
    private static final String REPORT_DIR = "crash-reports";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    private final ObjectMapper objectMapper;

    public CrashReporter(ConfigLoader configLoader) {
        this.configLoader = configLoader;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public String createReport(Throwable throwable, Method method) {
        try {
            Path reportDir = Paths.get(REPORT_DIR);
            if (!Files.exists(reportDir)) {
                Files.createDirectories(reportDir);
            }

            String timestamp = LocalDateTime.now().format(FORMATTER);
            String filename = String.format("crash-%s.json", timestamp);
            Path reportPath = reportDir.resolve(filename);

            CrashReport report = generateReport(throwable, method);
            objectMapper.writeValue(reportPath.toFile(), report);

            return reportPath.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("Failed to create crash report, error =  {}", e.getMessage());
            return null;
        }
    }

    private CrashReport generateReport(Throwable throwable, Method method) {
        CrashReport report = new CrashReport();
        AppMetadata metadata = configLoader.getMetadata();

        // Basic info
        report.setName(metadata.getName());
        report.setOperatingSystem(System.getProperty("os.name") + " " +
                System.getProperty("os.version"));
        report.setVersion(metadata.getVersion());
        report.setExplanation(
                "Well, this is embarrassing.\n\n" +
                        metadata.getName() + " had a problem and crashed. To help us diagnose " +
                        "the problem you can send us a crash report.\n\n" +
                        "We have generated a report file at the location below. Please include " +
                        "this file in your bug report."
        );
        report.setCause(throwable.getClass().getName() + ": " +
                (throwable.getMessage() != null ? throwable.getMessage() : "No message"));

        // Method info
        if (method != null) {
            CrashReport.MethodInfo methodInfo = new CrashReport.MethodInfo();
            methodInfo.setClassName(method.getDeclaringClass().getName());
            methodInfo.setMethodName(method.getName());
            report.setMethod(methodInfo);
        }

        // Stack trace
        List<CrashReport.StackFrame> frames = new ArrayList<>();
        for (StackTraceElement element : throwable.getStackTrace()) {
            CrashReport.StackFrame frame = new CrashReport.StackFrame();
            frame.setClassName(element.getClassName());
            frame.setMethod(element.getMethodName());
            frame.setFile(element.getFileName());
            frame.setLine(element.getLineNumber());
            frames.add(frame);
        }
        report.setBacktrace(frames);

        // System info
        CrashReport.SystemInfo systemInfo = new CrashReport.SystemInfo();
        Properties props = System.getProperties();
        systemInfo.setJavaVersion(props.getProperty("java.version"));
        systemInfo.setJavaVendor(props.getProperty("java.vendor"));
        systemInfo.setOsName(props.getProperty("os.name"));
        systemInfo.setOsVersion(props.getProperty("os.version"));
        systemInfo.setOsArch(props.getProperty("os.arch"));

        Runtime runtime = Runtime.getRuntime();
        systemInfo.setMaxMemoryMb(runtime.maxMemory() / 1024 / 1024);
        systemInfo.setTotalMemoryMb(runtime.totalMemory() / 1024 / 1024);
        systemInfo.setFreeMemoryMb(runtime.freeMemory() / 1024 / 1024);
        systemInfo.setProcessors(runtime.availableProcessors());

        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        systemInfo.setUptimeMs(runtimeMxBean.getUptime());

        report.setSystemInfo(systemInfo);

        // Application info
        Map<String, String> appInfo = new LinkedHashMap<>();
        appInfo.put("name", metadata.getName());
        appInfo.put("version", metadata.getVersion());
        appInfo.put("authors", String.join(", ", metadata.getAuthors()));
        if (!metadata.getHomepage().isEmpty()) {
            appInfo.put("homepage", metadata.getHomepage());
        }
        if (!metadata.getSupportUrl().isEmpty()) {
            appInfo.put("support", metadata.getSupportUrl());
        }
        if (!metadata.getIssueUrl().isEmpty()) {
            appInfo.put("issues", metadata.getIssueUrl());
        }
        report.setApplicationInfo(appInfo);

        return report;
    }
}