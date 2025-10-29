package io.pants.humanpanic.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Loads application metadata from various configuration sources
 */
@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class ConfigLoader {

    private final AppMetadata metadata;

    @Value("${app.name:Unknown Application}")
    private String appName;

    @Value("${app.version:Unknown Version}")
    private String appVersion;

    @Value("${app.authors:Unknown Authors}")
    private String appAuthors;

    @Value("${app.homepage:}")
    private String appHomepage;

    @Value("${app.support-url:}")
    private String appSupportUrl;

    @Value("${app.issue-url:}")
    private String appIssueUrl;

    @PostConstruct
    public void init() {
        if (metadata == null) {
            log.warn("AppMetadata is null, attempting fallback configuration");
            loadFallbackConfig();
            return;
        }

        metadata.setName(appName);
        metadata.setVersion(appVersion);

        // Parse authors (comma-separated)
        if (appAuthors != null && !appAuthors.isEmpty() && !appAuthors.equals("Unknown Authors")) {
            metadata.setAuthors(appAuthors.split(",\\s*"));
        } else {
            metadata.setAuthors(new String[]{"Unknown"});
        }

        metadata.setHomepage(appHomepage != null ? appHomepage : "");
        metadata.setSupportUrl(appSupportUrl != null ? appSupportUrl : "");
        metadata.setIssueUrl(appIssueUrl != null ? appIssueUrl : "");
    }

    /**
     * Fallback configuration loading for non-Spring environments
     */
    private void loadFallbackConfig() {
        // Try to load from manifest or system properties
        String name = System.getProperty("app.name");
        String version = System.getProperty("app.version");

        if (name != null && metadata != null) metadata.setName(name);
        if (version != null && metadata != null) metadata.setVersion(version);

        // Try reading from manifest
        try {
            Package pkg = getClass().getPackage();
            if (pkg != null && metadata != null) {
                String implTitle = pkg.getImplementationTitle();
                String implVersion = pkg.getImplementationVersion();

                if (implTitle != null) metadata.setName(implTitle);
                if (implVersion != null) metadata.setVersion(implVersion);
            }
        } catch (Exception e) {
            log.error("Failed to load config from manifest, error = {}", e.getMessage());
        }
    }
}