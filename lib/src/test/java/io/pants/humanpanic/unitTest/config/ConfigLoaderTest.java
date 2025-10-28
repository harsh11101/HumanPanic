package io.pants.humanpanic.unitTest.config;

import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.ConfigLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigLoader
 */
@ExtendWith(MockitoExtension.class)
class ConfigLoaderTest {

    private AppMetadata metadata;

    @BeforeEach
    void setUp() {
        metadata = new AppMetadata();
    }

    private ConfigLoader newLoader(
            String name,
            String version,
            String authors,
            String homepage,
            String supportUrl,
            String issueUrl
    ) {
        return new ConfigLoader(metadata, name, version, authors, homepage, supportUrl, issueUrl);
    }

    @Test
    void testDefaultValues() {
        ConfigLoader loader = newLoader(
                "Unknown Application",
                "Unknown Version",
                "Unknown Authors",
                "",
                "",
                ""
        );
        loader.init();

        assertNotNull(loader.getMetadata());
        assertEquals("Unknown Application", loader.getMetadata().getName());
        assertEquals("Unknown Version", loader.getMetadata().getVersion());
    }

    @Test
    void testSetAppName() {
        ConfigLoader loader = newLoader("Test Application", "Unknown Version", "Unknown Authors", "", "", "");
        loader.init();
        assertEquals("Test Application", metadata.getName());
    }

    @Test
    void testSetAppVersion() {
        ConfigLoader loader = newLoader("Unknown Application", "1.2.3", "Unknown Authors", "", "", "");
        loader.init();
        assertEquals("1.2.3", metadata.getVersion());
    }

    @Test
    void testSetAppAuthors_SingleAuthor() {
        ConfigLoader loader = newLoader("Unknown", "Unknown", "John Doe", "", "", "");
        loader.init();

        assertArrayEquals(new String[]{"John Doe"}, metadata.getAuthors());
    }

    @Test
    void testSetAppAuthors_MultipleAuthors() {
        ConfigLoader loader = newLoader("Unknown", "Unknown", "John Doe, Jane Smith, Bob Wilson", "", "", "");
        loader.init();

        assertArrayEquals(new String[]{"John Doe", "Jane Smith", "Bob Wilson"}, metadata.getAuthors());
    }

    @Test
    void testSetAppAuthors_WithSpaces() {
        ConfigLoader loader = newLoader("Unknown", "Unknown", "John Doe, Jane Smith, Bob Wilson", "", "", "");
        loader.init();

        assertArrayEquals(new String[]{"John Doe", "Jane Smith", "Bob Wilson"}, metadata.getAuthors());
    }

    @Test
    void testSetAppAuthors_EmptyString() {
        ConfigLoader loader = newLoader("Unknown", "Unknown", "", "", "", "");
        loader.init();

        assertArrayEquals(new String[]{"Unknown"}, metadata.getAuthors());
    }

    @Test
    void testSetAppAuthors_NullValue() {
        ConfigLoader loader = newLoader("Unknown", "Unknown", null, "", "", "");
        loader.init();

        assertArrayEquals(new String[]{"Unknown"}, metadata.getAuthors());
    }

    @Test
    void testSetAppHomepage() {
        ConfigLoader loader = newLoader("Unknown", "Unknown", "Unknown", "https://example.com", "", "");
        loader.init();

        assertEquals("https://example.com", metadata.getHomepage());
    }

    @Test
    void testSetAppHomepage_NullValue() {
        ConfigLoader loader = newLoader("Unknown", "Unknown", "Unknown", null, "", "");
        loader.init();

        assertEquals("", metadata.getHomepage());
    }

    @Test
    void testSetAppSupportUrl() {
        ConfigLoader loader = newLoader("Unknown", "Unknown", "Unknown", "", "https://example.com/support", "");
        loader.init();

        assertEquals("https://example.com/support", metadata.getSupportUrl());
    }

    @Test
    void testSetAppSupportUrl_NullValue() {
        ConfigLoader loader = newLoader("Unknown", "Unknown", "Unknown", "", null, "");
        loader.init();

        assertEquals("", metadata.getSupportUrl());
    }

    @Test
    void testSetAppIssueUrl() {
        ConfigLoader loader = newLoader("Unknown", "Unknown", "Unknown", "", "", "https://github.com/example/issues");
        loader.init();

        assertEquals("https://github.com/example/issues", metadata.getIssueUrl());
    }

    @Test
    void testSetAppIssueUrl_NullValue() {
        ConfigLoader loader = newLoader("Unknown", "Unknown", "Unknown", "", "", null);
        loader.init();

        assertEquals("", metadata.getIssueUrl());
    }

    @Test
    void testGetMetadata_NotNull() {
        ConfigLoader loader = newLoader("Unknown", "Unknown", "Unknown", "", "", "");
        loader.init();

        assertNotNull(loader.getMetadata());
    }

    @Test
    void testCompleteConfiguration() {
        ConfigLoader loader = newLoader(
                "My App",
                "2.0.0",
                "Alice, Bob",
                "https://myapp.com",
                "https://myapp.com/help",
                "https://github.com/myapp/issues"
        );
        loader.init();

        assertEquals("My App", metadata.getName());
        assertEquals("2.0.0", metadata.getVersion());
        assertArrayEquals(new String[]{"Alice", "Bob"}, metadata.getAuthors());
        assertEquals("https://myapp.com", metadata.getHomepage());
        assertEquals("https://myapp.com/help", metadata.getSupportUrl());
        assertEquals("https://github.com/myapp/issues", metadata.getIssueUrl());
    }

    @Test
    void testInitWithNullMetadata() {
        ConfigLoader loader = new ConfigLoader(
                null,
                "Unknown Application",
                "Unknown Version",
                "Unknown Authors",
                "",
                "",
                ""
        );

        assertDoesNotThrow(loader::init);
    }

    @Test
    void testMultipleInitCalls() {
        ConfigLoader loader = newLoader("First Name", "Unknown", "Unknown", "", "", "");
        loader.init();
        assertEquals("First Name", metadata.getName());

        loader = newLoader("Second Name", "Unknown", "Unknown", "", "", "");
        loader.init();
        assertEquals("Second Name", metadata.getName());
    }

    @Test
    void testEmptyConfigurationValues() {
        ConfigLoader loader = newLoader("", "", "", "", "", "");
        loader.init();

        assertEquals("", metadata.getName());
        assertEquals("", metadata.getVersion());
        assertEquals("", metadata.getHomepage());
        assertEquals("", metadata.getSupportUrl());
        assertEquals("", metadata.getIssueUrl());
    }
}
