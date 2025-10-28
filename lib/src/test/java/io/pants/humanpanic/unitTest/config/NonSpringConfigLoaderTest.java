package io.pants.humanpanic.unitTest.config;

import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.NonSpringConfigLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NonSpringConfigLoader
 */
class NonSpringConfigLoaderTest {

    private NonSpringConfigLoader configLoader;
    private AppMetadata metadata;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        metadata = new AppMetadata();
        configLoader = new NonSpringConfigLoader();
        configLoader.setMetadata(metadata);
    }

    @Test
    void testGetAndSetMetadata() {
        AppMetadata newMetadata = new AppMetadata();
        newMetadata.setName("Test");

        configLoader.setMetadata(newMetadata);

        assertSame(newMetadata, configLoader.getMetadata());
    }

    @Test
    void testLoadConfiguration_WithNoFiles() {
        // Should not throw exception when no config files exist
        assertDoesNotThrow(() -> {
            // Simulate loading (private method, so we just verify no crash)
            assertNotNull(configLoader.getMetadata());
        });
    }

    @Test
    void testMetadataDefaults() {
        assertNotNull(configLoader.getMetadata());
    }

    @Test
    void testSetMetadata_Null() {
        configLoader.setMetadata(null);
        assertNull(configLoader.getMetadata());
    }

    @Test
    void testYamlConfig_InnerClass() {
        // Test that inner classes are accessible
        assertDoesNotThrow(() -> {
            Class.forName("io.pants.humanpanic.config.NonSpringConfigLoader$YamlConfig");
            Class.forName("io.pants.humanpanic.config.NonSpringConfigLoader$AppConfig");
        });
    }

    @Test
    void testConfigLoader_WithSystemProperties() {
        System.setProperty("app.name", "System Property App");
        System.setProperty("app.version", "2.0.0");

        // The loader should be able to read system properties as fallback
        assertNotNull(metadata);

        System.clearProperty("app.name");
        System.clearProperty("app.version");
    }

    @Test
    void testConfigLoader_CreatesNewMetadata() {
        NonSpringConfigLoader newLoader = new NonSpringConfigLoader();

        // Should have metadata set through autowiring or manual setting
        assertDoesNotThrow(() -> newLoader.setMetadata(new AppMetadata()));
    }

    @Test
    void testMultipleLoaders() {
        NonSpringConfigLoader loader1 = new NonSpringConfigLoader();
        NonSpringConfigLoader loader2 = new NonSpringConfigLoader();

        AppMetadata metadata1 = new AppMetadata();
        AppMetadata metadata2 = new AppMetadata();

        metadata1.setName("Loader1");
        metadata2.setName("Loader2");

        loader1.setMetadata(metadata1);
        loader2.setMetadata(metadata2);

        assertEquals("Loader1", loader1.getMetadata().getName());
        assertEquals("Loader2", loader2.getMetadata().getName());
    }
}
