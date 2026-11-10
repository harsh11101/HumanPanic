package io.pants.humanpanic.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * The data written to a crash report file. The field names in the JSON file are given by the
 * {@link JsonProperty} annotations, so the class can be read back with Jackson if your application
 * has it. HumanPanic itself does not need Jackson.
 *
 * <p>This is a plain mutable data holder and is not thread-safe.
 */
public class CrashReport {
    @JsonProperty("name")
    private String name;

    @JsonProperty("operating_system")
    private String operatingSystem;

    @JsonProperty("version")
    private String version;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("thread")
    private String thread;

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("cause")
    private String cause;

    @JsonProperty("method")
    private MethodInfo method;

    @JsonProperty("backtrace")
    private List<StackFrame> backtrace;

    @JsonProperty("caused_by")
    private List<Cause> causedBy;

    @JsonProperty("system_info")
    private SystemInfo systemInfo;

    @JsonProperty("application_info")
    private Map<String, String> applicationInfo;

    /** Creates an empty report. */
    public CrashReport() {
    }

    /**
     * Returns the application name.
     *
     * @return the application name
     */
    public String getName() { return name; }

    /**
     * Sets the application name.
     *
     * @param name the application name
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the operating system name and version.
     *
     * @return the operating system name and version
     */
    public String getOperatingSystem() { return operatingSystem; }

    /**
     * Sets the operating system name and version.
     *
     * @param operatingSystem the operating system name and version
     */
    public void setOperatingSystem(String operatingSystem) { this.operatingSystem = operatingSystem; }

    /**
     * Returns the application version.
     *
     * @return the application version
     */
    public String getVersion() { return version; }

    /**
     * Sets the application version.
     *
     * @param version the application version
     */
    public void setVersion(String version) { this.version = version; }

    /**
     * Returns when the failure was handled, as an ISO-8601 offset date-time.
     *
     * @return when the failure was handled, as an ISO-8601 offset date-time
     */
    public String getTimestamp() { return timestamp; }

    /**
     * Sets when the failure was handled, as an ISO-8601 offset date-time.
     *
     * @param timestamp when the failure was handled, as an ISO-8601 offset date-time
     */
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    /**
     * Returns the name of the thread that handled the failure.
     *
     * @return the name of the thread that handled the failure
     */
    public String getThread() { return thread; }

    /**
     * Sets the name of the thread that handled the failure.
     *
     * @param thread the name of the thread that handled the failure
     */
    public void setThread(String thread) { this.thread = thread; }

    /**
     * Returns the human-readable explanation.
     *
     * @return the human-readable explanation
     */
    public String getExplanation() { return explanation; }

    /**
     * Sets the human-readable explanation.
     *
     * @param explanation the human-readable explanation
     */
    public void setExplanation(String explanation) { this.explanation = explanation; }

    /**
     * Returns the top-level failure, as {@code "fully.qualified.Type: message"}.
     *
     * @return the top-level failure, as {@code "fully.qualified.Type: message"}
     */
    public String getCause() { return cause; }

    /**
     * Sets the top-level failure, as {@code "fully.qualified.Type: message"}.
     *
     * @param cause the top-level failure, as {@code "fully.qualified.Type: message"}
     */
    public void setCause(String cause) { this.cause = cause; }

    /**
     * Returns the annotated method that failed, or {@code null} if unknown.
     *
     * @return the annotated method that failed, or {@code null} if unknown
     */
    public MethodInfo getMethod() { return method; }

    /**
     * Sets the annotated method that failed.
     *
     * @param method the annotated method that failed
     */
    public void setMethod(MethodInfo method) { this.method = method; }

    /**
     * Returns the stack trace of the top-level failure.
     *
     * @return the stack trace of the top-level failure
     */
    public List<StackFrame> getBacktrace() { return backtrace; }

    /**
     * Sets the stack trace of the top-level failure.
     *
     * @param backtrace the stack trace of the top-level failure
     */
    public void setBacktrace(List<StackFrame> backtrace) { this.backtrace = backtrace; }

    /**
     * Returns the cause chain, outermost cause first (empty if there is none).
     *
     * @return the cause chain, outermost cause first (empty if there is none)
     */
    public List<Cause> getCausedBy() { return causedBy; }

    /**
     * Sets the cause chain, outermost cause first.
     *
     * @param causedBy the cause chain, outermost cause first
     */
    public void setCausedBy(List<Cause> causedBy) { this.causedBy = causedBy; }

    /**
     * Returns the JVM and host information.
     *
     * @return the JVM and host information
     */
    public SystemInfo getSystemInfo() { return systemInfo; }

    /**
     * Sets the JVM and host information.
     *
     * @param systemInfo the JVM and host information
     */
    public void setSystemInfo(SystemInfo systemInfo) { this.systemInfo = systemInfo; }

    /**
     * Returns the application metadata (name, version, authors, URLs).
     *
     * @return the application metadata (name, version, authors, URLs)
     */
    public Map<String, String> getApplicationInfo() { return applicationInfo; }

    /**
     * Sets the application metadata.
     *
     * @param applicationInfo the application metadata
     */
    public void setApplicationInfo(Map<String, String> applicationInfo) { this.applicationInfo = applicationInfo; }

    /** The annotated method in which the failure was intercepted. */
    public static class MethodInfo {
        @JsonProperty("class")
        private String className;
        @JsonProperty("method")
        private String methodName;

        /** Creates an empty instance. */
        public MethodInfo() {
        }

        /**
         * Returns the declaring class name.
         *
         * @return the declaring class name
         */
        public String getClassName() { return className; }

        /**
         * Sets the declaring class name.
         *
         * @param className the declaring class name
         */
        public void setClassName(String className) { this.className = className; }

        /**
         * Returns the method name.
         *
         * @return the method name
         */
        public String getMethodName() { return methodName; }

        /**
         * Sets the method name.
         *
         * @param methodName the method name
         */
        public void setMethodName(String methodName) { this.methodName = methodName; }
    }

    /** One frame of a stack trace. */
    public static class StackFrame {
        @JsonProperty("class")
        private String className;
        @JsonProperty("method")
        private String method;
        @JsonProperty("file")
        private String file;
        @JsonProperty("line")
        private Integer line;

        /** Creates an empty instance. */
        public StackFrame() {
        }

        /**
         * Returns the class name.
         *
         * @return the class name
         */
        public String getClassName() { return className; }

        /**
         * Sets the class name.
         *
         * @param className the class name
         */
        public void setClassName(String className) { this.className = className; }

        /**
         * Returns the method name.
         *
         * @return the method name
         */
        public String getMethod() { return method; }

        /**
         * Sets the method name.
         *
         * @param method the method name
         */
        public void setMethod(String method) { this.method = method; }

        /**
         * Returns the source file name, or {@code null} if unknown.
         *
         * @return the source file name, or {@code null} if unknown
         */
        public String getFile() { return file; }

        /**
         * Sets the source file name.
         *
         * @param file the source file name
         */
        public void setFile(String file) { this.file = file; }

        /**
         * Returns the line number; negative if unknown, {@code -2} for native methods.
         *
         * @return the line number; negative if unknown, {@code -2} for native methods
         */
        public Integer getLine() { return line; }

        /**
         * Sets the line number.
         *
         * @param line the line number
         */
        public void setLine(Integer line) { this.line = line; }
    }

    /** One entry in the cause chain of the failure. */
    public static class Cause {
        @JsonProperty("cause")
        private String cause;
        @JsonProperty("backtrace")
        private List<StackFrame> backtrace;

        /** Creates an empty instance. */
        public Cause() {
        }

        /**
         * Returns the cause, as {@code "fully.qualified.Type: message"}.
         *
         * @return the cause, as {@code "fully.qualified.Type: message"}
         */
        public String getCause() { return cause; }

        /**
         * Sets the cause, as {@code "fully.qualified.Type: message"}.
         *
         * @param cause the cause, as {@code "fully.qualified.Type: message"}
         */
        public void setCause(String cause) { this.cause = cause; }

        /**
         * Returns the stack trace of this cause.
         *
         * @return the stack trace of this cause
         */
        public List<StackFrame> getBacktrace() { return backtrace; }

        /**
         * Sets the stack trace of this cause.
         *
         * @param backtrace the stack trace of this cause
         */
        public void setBacktrace(List<StackFrame> backtrace) { this.backtrace = backtrace; }
    }

    /** JVM and host information at the time of the failure. */
    public static class SystemInfo {
        @JsonProperty("java_version")
        private String javaVersion;
        @JsonProperty("java_vendor")
        private String javaVendor;
        @JsonProperty("os_name")
        private String osName;
        @JsonProperty("os_version")
        private String osVersion;
        @JsonProperty("os_arch")
        private String osArch;
        @JsonProperty("max_memory_mb")
        private long maxMemoryMb;
        @JsonProperty("total_memory_mb")
        private long totalMemoryMb;
        @JsonProperty("free_memory_mb")
        private long freeMemoryMb;
        @JsonProperty("processors")
        private int processors;
        @JsonProperty("uptime_ms")
        private long uptimeMs;

        /** Creates an empty instance. */
        public SystemInfo() {
        }

        /**
         * Returns the {@code java.version} system property.
         *
         * @return the {@code java.version} system property
         */
        public String getJavaVersion() { return javaVersion; }

        /**
         * Sets the Java version.
         *
         * @param javaVersion the Java version
         */
        public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }

        /**
         * Returns the {@code java.vendor} system property.
         *
         * @return the {@code java.vendor} system property
         */
        public String getJavaVendor() { return javaVendor; }

        /**
         * Sets the Java vendor.
         *
         * @param javaVendor the Java vendor
         */
        public void setJavaVendor(String javaVendor) { this.javaVendor = javaVendor; }

        /**
         * Returns the {@code os.name} system property.
         *
         * @return the {@code os.name} system property
         */
        public String getOsName() { return osName; }

        /**
         * Sets the OS name.
         *
         * @param osName the OS name
         */
        public void setOsName(String osName) { this.osName = osName; }

        /**
         * Returns the {@code os.version} system property.
         *
         * @return the {@code os.version} system property
         */
        public String getOsVersion() { return osVersion; }

        /**
         * Sets the OS version.
         *
         * @param osVersion the OS version
         */
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }

        /**
         * Returns the {@code os.arch} system property.
         *
         * @return the {@code os.arch} system property
         */
        public String getOsArch() { return osArch; }

        /**
         * Sets the OS architecture.
         *
         * @param osArch the OS architecture
         */
        public void setOsArch(String osArch) { this.osArch = osArch; }

        /**
         * Returns the maximum heap size in MiB.
         *
         * @return the maximum heap size in MiB
         */
        public long getMaxMemoryMb() { return maxMemoryMb; }

        /**
         * Sets the maximum heap size in MiB.
         *
         * @param maxMemoryMb the maximum heap size in MiB
         */
        public void setMaxMemoryMb(long maxMemoryMb) { this.maxMemoryMb = maxMemoryMb; }

        /**
         * Returns the current heap size in MiB.
         *
         * @return the current heap size in MiB
         */
        public long getTotalMemoryMb() { return totalMemoryMb; }

        /**
         * Sets the current heap size in MiB.
         *
         * @param totalMemoryMb the current heap size in MiB
         */
        public void setTotalMemoryMb(long totalMemoryMb) { this.totalMemoryMb = totalMemoryMb; }

        /**
         * Returns the free heap in MiB.
         *
         * @return the free heap in MiB
         */
        public long getFreeMemoryMb() { return freeMemoryMb; }

        /**
         * Sets the free heap in MiB.
         *
         * @param freeMemoryMb the free heap in MiB
         */
        public void setFreeMemoryMb(long freeMemoryMb) { this.freeMemoryMb = freeMemoryMb; }

        /**
         * Returns the number of available processors.
         *
         * @return the number of available processors
         */
        public int getProcessors() { return processors; }

        /**
         * Sets the number of available processors.
         *
         * @param processors the number of available processors
         */
        public void setProcessors(int processors) { this.processors = processors; }

        /**
         * Returns the JVM uptime in milliseconds, or {@code -1} if unavailable.
         *
         * @return the JVM uptime in milliseconds, or {@code -1} if unavailable
         */
        public long getUptimeMs() { return uptimeMs; }

        /**
         * Sets the JVM uptime in milliseconds.
         *
         * @param uptimeMs the JVM uptime in milliseconds
         */
        public void setUptimeMs(long uptimeMs) { this.uptimeMs = uptimeMs; }
    }
}
