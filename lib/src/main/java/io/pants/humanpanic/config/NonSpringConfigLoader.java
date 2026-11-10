package io.pants.humanpanic.config;

import io.pants.humanpanic.internal.Diagnostics;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads {@link AppMetadata} from {@code app.*} properties without any framework. It uses the same
 * property names as {@link ConfigLoader}.
 *
 * <p>{@link #load()} reads, in increasing order of precedence:
 * <ol>
 *   <li>{@code application.properties} on the classpath (via the thread context class loader), or,
 *       if it is not there, {@code ./application.properties} in the working directory;</li>
 *   <li>JVM system properties ({@code -Dapp.name=...}).</li>
 * </ol>
 * Properties files are read as UTF-8. A missing or unreadable file is not an error: defaults are
 * used.
 *
 * <p>Instances are not thread-safe.
 */
public class NonSpringConfigLoader {

    static final String PROPERTIES_FILE = "application.properties";
    private static final System.Logger LOG = System.getLogger(NonSpringConfigLoader.class.getName());

    private AppMetadata metadata = new AppMetadata();

    /** Creates a loader holding default metadata. */
    public NonSpringConfigLoader() {
    }

    /**
     * Loads metadata from {@code application.properties} and system properties (see the class
     * documentation), stores it in this loader and returns it.
     *
     * @return the loaded metadata, never {@code null}
     */
    public AppMetadata load() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = NonSpringConfigLoader.class.getClassLoader();
        }
        return load(classLoader, Paths.get(PROPERTIES_FILE));
    }

    /**
     * Builds metadata only from the given properties (system properties are <em>not</em> consulted),
     * stores it in this loader and returns it.
     *
     * @param properties the properties holding {@code app.*} keys; must not be {@code null}
     * @return the loaded metadata, never {@code null}
     */
    public AppMetadata load(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        this.metadata = toMetadata(properties);
        return metadata;
    }

    AppMetadata load(ClassLoader classLoader, Path fallbackFile) {
        Properties properties = new Properties();
        readInto(properties, classLoader, fallbackFile);
        for (String key : System.getProperties().stringPropertyNames()) {
            if (key.startsWith("app.")) {
                properties.setProperty(key, System.getProperty(key));
            }
        }
        this.metadata = toMetadata(properties);
        return metadata;
    }

    private static void readInto(Properties properties, ClassLoader classLoader, Path fallbackFile) {
        try (InputStream resource = classLoader == null ? null : classLoader.getResourceAsStream(PROPERTIES_FILE)) {
            if (resource != null) {
                load(properties, resource);
                return;
            }
            if (Files.isRegularFile(fallbackFile)) {
                try (InputStream file = Files.newInputStream(fallbackFile)) {
                    load(properties, file);
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            Diagnostics.warn(LOG, "HumanPanic: could not read " + PROPERTIES_FILE + "; using defaults", e);
        }
    }

    private static void load(Properties properties, InputStream in) throws IOException {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
    }

    private static AppMetadata toMetadata(Properties p) {
        AppMetadata m = new AppMetadata();
        m.setName(p.getProperty("app.name", m.getName()));
        m.setVersion(p.getProperty("app.version", m.getVersion()));
        m.setAuthors(AppMetadata.parseAuthors(p.getProperty("app.authors")));
        m.setHomepage(p.getProperty("app.homepage", ""));
        m.setSupportUrl(p.getProperty("app.support-url", ""));
        m.setIssueUrl(p.getProperty("app.issue-url", ""));
        return m;
    }

    /**
     * Returns the metadata produced by the last {@code load} call, or the metadata set explicitly.
     *
     * @return the current metadata (default metadata until loaded)
     */
    public AppMetadata getMetadata() {
        return metadata;
    }

    /**
     * Replaces the current metadata.
     *
     * @param metadata the metadata
     */
    public void setMetadata(AppMetadata metadata) {
        this.metadata = metadata;
    }
}
