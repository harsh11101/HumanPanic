package io.pants.humanpanic.reporter;

import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.ConfigLoader;
import io.pants.humanpanic.internal.Diagnostics;
import io.pants.humanpanic.internal.Json;
import io.pants.humanpanic.model.CrashReport;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Writes JSON crash reports, in the spirit of Rust's {@code human-panic}.
 *
 * <p>Reports are named {@code crash-<yyyy-MM-dd-HH-mm-ss-SSS>-<random>.json} and are created
 * atomically with {@code CREATE_NEW}, so concurrent failures never overwrite each other. The report
 * directory is created on demand.
 *
 * <p><b>Fail-safe:</b> {@link #createReport(Throwable, Method)} never throws. If the report cannot be
 * written, it logs a warning through {@link System.Logger} and returns {@code null}.
 *
 * <p><b>Privacy:</b> reports contain exception messages and stack traces, which may include
 * sensitive data. They are written with the default file permissions of the process and are never
 * sent anywhere.
 *
 * <p>Instances are thread-safe.
 */
public class CrashReporter {

    /** Directory used when none is configured, relative to the working directory. */
    public static final String DEFAULT_REPORT_DIRECTORY = "crash-reports";

    private static final System.Logger LOG = System.getLogger(CrashReporter.class.getName());
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS");
    private static final int MAX_CAUSE_DEPTH = 32;
    private static final int MAX_NAME_ATTEMPTS = 8;

    private final Supplier<AppMetadata> metadata;
    private final Path reportDirectory;

    /**
     * Creates a reporter that reads metadata from a {@link ConfigLoader} (Spring integration) and
     * writes to its {@code humanpanic.report-dir} (default {@value #DEFAULT_REPORT_DIRECTORY}).
     *
     * @param configLoader the metadata source; must not be {@code null}
     */
    public CrashReporter(ConfigLoader configLoader) {
        Objects.requireNonNull(configLoader, "configLoader");
        this.metadata = configLoader::getMetadata;
        this.reportDirectory = Paths.get(configLoader.getReportDirectory());
    }

    /**
     * Creates a reporter with explicit metadata and report directory.
     *
     * @param metadata        the application metadata; must not be {@code null}
     * @param reportDirectory where reports are written; must not be {@code null}
     */
    public CrashReporter(AppMetadata metadata, Path reportDirectory) {
        Objects.requireNonNull(metadata, "metadata");
        this.metadata = () -> metadata;
        this.reportDirectory = Objects.requireNonNull(reportDirectory, "reportDirectory");
    }

    /**
     * Returns the directory reports are written to.
     *
     * @return the report directory (possibly relative)
     */
    public Path getReportDirectory() {
        return reportDirectory;
    }

    /**
     * Writes a crash report for the given failure.
     *
     * @param throwable the failure; if {@code null}, no report is written
     * @param method    the intercepted method, or {@code null} if not applicable
     * @return the absolute path of the written report, or {@code null} if it could not be written
     */
    public String createReport(Throwable throwable, Method method) {
        if (throwable == null) {
            LOG.log(System.Logger.Level.WARNING, "HumanPanic: createReport called without a throwable");
            return null;
        }
        try {
            OffsetDateTime now = OffsetDateTime.now();
            byte[] json = Json.write(toJsonTree(generateReport(throwable, method, now)))
                    .getBytes(StandardCharsets.UTF_8);
            Files.createDirectories(reportDirectory);
            return write(json, now).toAbsolutePath().toString();
        } catch (IOException | RuntimeException | LinkageError e) {
            Diagnostics.warn(LOG, "HumanPanic: failed to write crash report to "
                    + reportDirectory.toAbsolutePath(), e);
            return null;
        }
    }

    private Path write(byte[] json, OffsetDateTime now) throws IOException {
        String stamp = now.format(FILE_TIMESTAMP);
        for (int attempt = 1; ; attempt++) {
            String suffix = Integer.toHexString(ThreadLocalRandom.current().nextInt(0x100000, 0x1000000));
            Path path = reportDirectory.resolve("crash-" + stamp + "-" + suffix + ".json");
            try {
                // createFile is atomic: it fails if the name is taken, so reports never overwrite.
                return Files.write(Files.createFile(path, ownerOnly(path)), json);
            } catch (FileAlreadyExistsException e) {
                if (attempt >= MAX_NAME_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    /**
     * Reports can contain sensitive data, so on POSIX file systems they are readable and writable
     * by the owner only. Elsewhere (Windows) the directory's permissions apply.
     */
    private static FileAttribute<?>[] ownerOnly(Path path) {
        if (path.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            return new FileAttribute<?>[]{
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))};
        }
        return new FileAttribute<?>[0];
    }

    private CrashReport generateReport(Throwable throwable, Method method, OffsetDateTime now) {
        AppMetadata md = metadata.get();
        String name = orDefault(md == null ? null : md.getName(), AppMetadata.DEFAULT_NAME);
        String version = orDefault(md == null ? null : md.getVersion(), AppMetadata.DEFAULT_VERSION);

        CrashReport report = new CrashReport();
        report.setName(name);
        report.setOperatingSystem(System.getProperty("os.name") + " " + System.getProperty("os.version"));
        report.setVersion(version);
        report.setTimestamp(now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        report.setThread(Thread.currentThread().getName());
        report.setExplanation(
                "Well, this is embarrassing.\n\n"
                        + name + " had a problem and crashed. To help us diagnose "
                        + "the problem you can send us a crash report.\n\n"
                        + "Please include this file in your bug report.");
        report.setCause(describe(throwable));

        if (method != null) {
            CrashReport.MethodInfo methodInfo = new CrashReport.MethodInfo();
            methodInfo.setClassName(method.getDeclaringClass().getName());
            methodInfo.setMethodName(method.getName());
            report.setMethod(methodInfo);
        }

        report.setBacktrace(frames(throwable));
        report.setCausedBy(causes(throwable));
        report.setSystemInfo(systemInfo());

        Map<String, String> appInfo = new LinkedHashMap<>();
        appInfo.put("name", name);
        appInfo.put("version", version);
        String[] authors = md == null ? null : md.getAuthors();
        if (authors != null && authors.length > 0) {
            appInfo.put("authors", String.join(", ", authors));
        }
        putIfPresent(appInfo, "homepage", md == null ? null : md.getHomepage());
        putIfPresent(appInfo, "support", md == null ? null : md.getSupportUrl());
        putIfPresent(appInfo, "issues", md == null ? null : md.getIssueUrl());
        report.setApplicationInfo(appInfo);
        return report;
    }

    private static List<CrashReport.Cause> causes(Throwable throwable) {
        List<CrashReport.Cause> causes = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        seen.add(throwable);
        Throwable cause = throwable.getCause();
        while (cause != null && seen.add(cause) && causes.size() < MAX_CAUSE_DEPTH) {
            CrashReport.Cause c = new CrashReport.Cause();
            c.setCause(describe(cause));
            c.setBacktrace(frames(cause));
            causes.add(c);
            cause = cause.getCause();
        }
        return causes;
    }

    private static List<CrashReport.StackFrame> frames(Throwable throwable) {
        StackTraceElement[] trace = throwable.getStackTrace();
        List<CrashReport.StackFrame> frames = new ArrayList<>(trace.length);
        for (StackTraceElement element : trace) {
            CrashReport.StackFrame frame = new CrashReport.StackFrame();
            frame.setClassName(element.getClassName());
            frame.setMethod(element.getMethodName());
            frame.setFile(element.getFileName());
            frame.setLine(element.getLineNumber());
            frames.add(frame);
        }
        return frames;
    }

    private static CrashReport.SystemInfo systemInfo() {
        CrashReport.SystemInfo info = new CrashReport.SystemInfo();
        info.setJavaVersion(System.getProperty("java.version"));
        info.setJavaVendor(System.getProperty("java.vendor"));
        info.setOsName(System.getProperty("os.name"));
        info.setOsVersion(System.getProperty("os.version"));
        info.setOsArch(System.getProperty("os.arch"));
        Runtime runtime = Runtime.getRuntime();
        info.setMaxMemoryMb(runtime.maxMemory() / 1024 / 1024);
        info.setTotalMemoryMb(runtime.totalMemory() / 1024 / 1024);
        info.setFreeMemoryMb(runtime.freeMemory() / 1024 / 1024);
        info.setProcessors(runtime.availableProcessors());
        info.setUptimeMs(uptime());
        return info;
    }

    private static long uptime() {
        try {
            return ManagementFactory.getRuntimeMXBean().getUptime();
        } catch (LinkageError | RuntimeException e) {
            // java.management may be absent from a jlink'ed runtime image.
            return -1L;
        }
    }

    private static String describe(Throwable t) {
        String message = t.getMessage();
        return t.getClass().getName() + ": " + (message != null ? message : "No message");
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    // Field names must match the @JsonProperty annotations on CrashReport (verified by tests).
    private static Map<String, Object> toJsonTree(CrashReport r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", r.getName());
        m.put("operating_system", r.getOperatingSystem());
        m.put("version", r.getVersion());
        m.put("timestamp", r.getTimestamp());
        m.put("thread", r.getThread());
        m.put("explanation", r.getExplanation());
        m.put("cause", r.getCause());
        CrashReport.MethodInfo method = r.getMethod();
        if (method == null) {
            m.put("method", null);
        } else {
            Map<String, Object> mi = new LinkedHashMap<>();
            mi.put("class", method.getClassName());
            mi.put("method", method.getMethodName());
            m.put("method", mi);
        }
        m.put("backtrace", framesTree(r.getBacktrace()));
        List<Object> causes = new ArrayList<>();
        for (CrashReport.Cause c : r.getCausedBy()) {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("cause", c.getCause());
            cm.put("backtrace", framesTree(c.getBacktrace()));
            causes.add(cm);
        }
        m.put("caused_by", causes);
        CrashReport.SystemInfo s = r.getSystemInfo();
        Map<String, Object> sm = new LinkedHashMap<>();
        sm.put("java_version", s.getJavaVersion());
        sm.put("java_vendor", s.getJavaVendor());
        sm.put("os_name", s.getOsName());
        sm.put("os_version", s.getOsVersion());
        sm.put("os_arch", s.getOsArch());
        sm.put("max_memory_mb", s.getMaxMemoryMb());
        sm.put("total_memory_mb", s.getTotalMemoryMb());
        sm.put("free_memory_mb", s.getFreeMemoryMb());
        sm.put("processors", s.getProcessors());
        sm.put("uptime_ms", s.getUptimeMs());
        m.put("system_info", sm);
        m.put("application_info", new LinkedHashMap<>(r.getApplicationInfo()));
        return m;
    }

    private static List<Object> framesTree(List<CrashReport.StackFrame> frames) {
        List<Object> list = new ArrayList<>(frames.size());
        for (CrashReport.StackFrame f : frames) {
            Map<String, Object> fm = new LinkedHashMap<>();
            fm.put("class", f.getClassName());
            fm.put("method", f.getMethod());
            fm.put("file", f.getFile());
            fm.put("line", f.getLine());
            list.add(fm);
        }
        return list;
    }
}
