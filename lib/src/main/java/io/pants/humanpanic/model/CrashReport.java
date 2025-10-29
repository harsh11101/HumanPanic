package io.pants.humanpanic.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CrashReport {
    @JsonProperty("name")
    private String name;

    @JsonProperty("operating_system")
    private String operatingSystem;

    @JsonProperty("version")
    private String version;

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("cause")
    private String cause;

    @JsonProperty("method")
    private MethodInfo method;

    @JsonProperty("backtrace")
    private List<StackFrame> backtrace;

    @JsonProperty("system_info")
    private SystemInfo systemInfo;

    @JsonProperty("application_info")
    private Map<String, String> applicationInfo;

    @Getter
    @Setter
    public static class MethodInfo {
        @JsonProperty("class")
        private String className;
        @JsonProperty("method")
        private String methodName;
    }

    @Getter
    @Setter
    public static class StackFrame {
        @JsonProperty("class")
        private String className;
        @JsonProperty("method")
        private String method;
        @JsonProperty("file")
        private String file;
        @JsonProperty("line")
        private Integer line;
    }

    @Getter
    @Setter
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
    }
}
