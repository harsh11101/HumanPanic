package io.pants.humanpanic.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

/**
 * <b>Spring integration.</b> Copies the {@code app.*} properties from the Spring
 * {@code Environment} into an {@link AppMetadata} bean.
 *
 * <table>
 *   <caption>Supported properties</caption>
 *   <tr><th>Property</th><th>Default</th></tr>
 *   <tr><td>{@code app.name}</td><td>{@value AppMetadata#DEFAULT_NAME}</td></tr>
 *   <tr><td>{@code app.version}</td><td>{@value AppMetadata#DEFAULT_VERSION}</td></tr>
 *   <tr><td>{@code app.authors}</td><td>none (comma-separated)</td></tr>
 *   <tr><td>{@code app.homepage}</td><td>none</td></tr>
 *   <tr><td>{@code app.support-url}</td><td>none</td></tr>
 *   <tr><td>{@code app.issue-url}</td><td>none</td></tr>
 * </table>
 *
 * <p>This class is registered by {@link HumanPanicConfiguration}. It needs {@code spring-beans} on
 * the classpath. Outside Spring, use {@link NonSpringConfigLoader} or build an {@link AppMetadata}
 * directly.
 */
public class ConfigLoader implements InitializingBean {

    private static final String DEFAULT_REPORT_DIRECTORY = "crash-reports";

    private final AppMetadata metadata;

    @Value("${app.name:" + AppMetadata.DEFAULT_NAME + "}")
    private String appName;

    @Value("${app.version:" + AppMetadata.DEFAULT_VERSION + "}")
    private String appVersion;

    @Value("${app.authors:}")
    private String appAuthors;

    @Value("${app.homepage:}")
    private String appHomepage;

    @Value("${app.support-url:}")
    private String appSupportUrl;

    @Value("${app.issue-url:}")
    private String appIssueUrl;

    @Value("${humanpanic.report-dir:" + DEFAULT_REPORT_DIRECTORY + "}")
    private String reportDirectory;

    /**
     * Creates a loader that populates the given metadata instance.
     *
     * @param metadata the metadata to populate; if {@code null}, a new instance is used
     */
    public ConfigLoader(AppMetadata metadata) {
        this.metadata = metadata != null ? metadata : new AppMetadata();
    }

    /** Called by Spring after property injection; delegates to {@link #init()}. */
    @Override
    public void afterPropertiesSet() {
        init();
    }

    /**
     * Applies the injected property values to the metadata. A {@code null} name or version (no
     * injection happened) leaves the current value unchanged. This method is idempotent.
     */
    public void init() {
        if (appName != null) {
            metadata.setName(appName);
        }
        if (appVersion != null) {
            metadata.setVersion(appVersion);
        }
        metadata.setAuthors(AppMetadata.parseAuthors(appAuthors));
        metadata.setHomepage(nullToEmpty(appHomepage));
        metadata.setSupportUrl(nullToEmpty(appSupportUrl));
        metadata.setIssueUrl(nullToEmpty(appIssueUrl));
    }

    /**
     * Returns the populated metadata.
     *
     * @return the metadata instance, never {@code null}
     */
    public AppMetadata getMetadata() {
        return metadata;
    }

    /**
     * Returns the raw {@code app.name} value.
     *
     * @return the raw {@code app.name} value
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Returns the raw {@code app.version} value.
     *
     * @return the raw {@code app.version} value
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Returns the raw {@code app.authors} value.
     *
     * @return the raw {@code app.authors} value
     */
    public String getAppAuthors() {
        return appAuthors;
    }

    /**
     * Returns the raw {@code app.homepage} value.
     *
     * @return the raw {@code app.homepage} value
     */
    public String getAppHomepage() {
        return appHomepage;
    }

    /**
     * Returns the raw {@code app.support-url} value.
     *
     * @return the raw {@code app.support-url} value
     */
    public String getAppSupportUrl() {
        return appSupportUrl;
    }

    /**
     * Returns the raw {@code app.issue-url} value.
     *
     * @return the raw {@code app.issue-url} value
     */
    public String getAppIssueUrl() {
        return appIssueUrl;
    }

    /**
     * Returns the directory crash reports are written to: the {@code humanpanic.report-dir}
     * property, or {@code crash-reports} if it is not set.
     *
     * @return the report directory; never blank
     */
    public String getReportDirectory() {
        return reportDirectory == null || reportDirectory.isBlank() ? DEFAULT_REPORT_DIRECTORY : reportDirectory;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
