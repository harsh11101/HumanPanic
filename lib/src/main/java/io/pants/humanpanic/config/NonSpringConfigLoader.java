package io.pants.humanpanic.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration loader for non-Spring environments.
 * Use this when Spring context is not available.
 */
@Component
@Getter
@Setter
public class NonSpringConfigLoader {

    @Autowired
    private AppMetadata metadata;

    private void loadConfiguration() {
        metadata = new AppMetadata();

        // Try loading from application.yml
        if (tryLoadYaml()) {
            return;
        }

        // Try loading from application.properties
        if (tryLoadProperties()) {
            return;
        }

        // Try loading from META-INF/MANIFEST.MF
        tryLoadManifest();
    }

    private boolean tryLoadYaml() {
        try {
            InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("application.yml");

            if (is == null) {
                Path yamlPath = Paths.get("application.yml");
                if (Files.exists(yamlPath)) {
                    is = Files.newInputStream(yamlPath);
                }
            }

            if (is != null) {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                YamlConfig config = mapper.readValue(is, YamlConfig.class);

                if (config.getApp() != null) {
                    AppConfig app = config.getApp();
                    if (app.getName() != null) metadata.setName(app.getName());
                    if (app.getVersion() != null) metadata.setVersion(app.getVersion());
                    if (app.getAuthors() != null) metadata.setAuthors(app.getAuthors());
                    if (app.getHomepage() != null) metadata.setHomepage(app.getHomepage());
                    if (app.getSupportUrl() != null) metadata.setSupportUrl(app.getSupportUrl());
                    if (app.getIssueUrl() != null) metadata.setIssueUrl(app.getIssueUrl());
                    return true;
                }
            }
        } catch (Exception e) {
            // Silent failure
        }
        return false;
    }

    private boolean tryLoadProperties() {
        try {
            Properties props = new Properties();
            InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("application.properties");

            if (is == null) {
                Path propsPath = Paths.get("application.properties");
                if (Files.exists(propsPath)) {
                    is = Files.newInputStream(propsPath);
                }
            }

            if (is != null) {
                props.load(is);
                metadata.setName(props.getProperty("app.name", metadata.getName()));
                metadata.setVersion(props.getProperty("app.version", metadata.getVersion()));

                String authors = props.getProperty("app.authors");
                if (authors != null) {
                    metadata.setAuthors(authors.split(",\\s*"));
                }

                metadata.setHomepage(props.getProperty("app.homepage", metadata.getHomepage()));
                metadata.setSupportUrl(props.getProperty("app.support-url", metadata.getSupportUrl()));
                metadata.setIssueUrl(props.getProperty("app.issue-url", metadata.getIssueUrl()));
                return true;
            }
        } catch (Exception e) {
            // Silent failure
        }
        return false;
    }

    private void tryLoadManifest() {
        try {
            Package pkg = getClass().getPackage();
            if (pkg != null) {
                String name = pkg.getImplementationTitle();
                String version = pkg.getImplementationVersion();

                if (name != null) metadata.setName(name);
                if (version != null) metadata.setVersion(version);
            }
        } catch (Exception e) {
            // Silent failure
        }
    }

    // Helper classes for YAML parsing
    @Getter
    @Setter
    private static class YamlConfig {
        private AppConfig app;
    }

    @Getter
    @Setter
    private static class AppConfig {
        private String name;
        private String version;
        private String[] authors;
        private String homepage;
        private String supportUrl;
        private String issueUrl;
    }
}
