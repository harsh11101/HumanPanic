package io.pants.humanpanic.unitTest.config;

import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.ConfigLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigLoader
 */
class ConfigLoaderTest {

    private AppMetadata metadata;
    private ConfigLoader configLoader;

    @BeforeEach
    void setUp() {
        metadata = new AppMetadata();
        configLoader = new ConfigLoader(metadata);
    }

    private void setFieldsAndInit(String name, String version, String authors,
                                  String homepage, String supportUrl, String issueUrl) {
        ReflectionTestUtils.setField(configLoader, "appName", name);
        ReflectionTestUtils.setField(configLoader, "appVersion", version);
        ReflectionTestUtils.setField(configLoader, "appAuthors", authors);
        ReflectionTestUtils.setField(configLoader, "appHomepage", homepage);
        ReflectionTestUtils.setField(configLoader, "appSupportUrl", supportUrl);
        ReflectionTestUtils.setField(configLoader, "appIssueUrl", issueUrl);
        configLoader.init();
    }

    @Test
    void testDefaultValues() {
        setFieldsAndInit("Unknown Application", "Unknown Version", "Unknown Authors", "", "", "");

        assertNotNull(configLoader.getMetadata());
        assertEquals("Unknown Application", configLoader.getMetadata().getName());
        assertEquals("Unknown Version", configLoader.getMetadata().getVersion());
    }

    @Test
    void testSetAppName() {
        setFieldsAndInit("Test Application", "Unknown Version", "Unknown Authors", "", "", "");
        assertEquals("Test Application", metadata.getName());
    }

    @Test
    void testSetAppVersion() {
        setFieldsAndInit("Unknown Application", "1.2.3", "Unknown Authors", "", "", "");
        assertEquals("1.2.3", metadata.getVersion());
    }

    @Test
    void testSetAppAuthors_SingleAuthor() {
        setFieldsAndInit("Unknown", "Unknown", "John Doe", "", "", "");
        assertArrayEquals(new String[]{"John Doe"}, metadata.getAuthors());
    }

    @Test
    void testSetAppAuthors_MultipleAuthors() {
        setFieldsAndInit("Unknown", "Unknown", "John Doe, Jane Smith, Bob Wilson", "", "", "");
        assertArrayEquals(new String[]{"John Doe", "Jane Smith", "Bob Wilson"}, metadata.getAuthors());
    }

    @Test
    void testSetAppAuthors_WithSpaces() {
        setFieldsAndInit("Unknown", "Unknown", "John Doe, Jane Smith, Bob Wilson", "", "", "");
        assertArrayEquals(new String[]{"John Doe", "Jane Smith", "Bob Wilson"}, metadata.getAuthors());
    }

    @Test
    void testSetAppAuthors_EmptyString() {
        setFieldsAndInit("Unknown", "Unknown", "", "", "", "");
        assertArrayEquals(new String[]{"Unknown"}, metadata.getAuthors());
    }

    @Test
    void testSetAppAuthors_NullValue() {
        setFieldsAndInit("Unknown", "Unknown", null, "", "", "");
        assertArrayEquals(new String[]{"Unknown"}, metadata.getAuthors());
    }

    @Test
    void testSetAppHomepage() {
        setFieldsAndInit("Unknown", "Unknown", "Unknown", "https://example.com", "", "");
        assertEquals("https://example.com", metadata.getHomepage());
    }

    @Test
    void testSetAppHomepage_NullValue() {
        setFieldsAndInit("Unknown", "Unknown", "Unknown", null, "", "");
        assertEquals("", metadata.getHomepage());
    }

    @Test
    void testSetAppSupportUrl() {
        setFieldsAndInit("Unknown", "Unknown", "Unknown", "", "https://example.com/support", "");
        assertEquals("https://example.com/support", metadata.getSupportUrl());
    }

    @Test
    void testSetAppSupportUrl_NullValue() {
        setFieldsAndInit("Unknown", "Unknown", "Unknown", "", null, "");
        assertEquals("", metadata.getSupportUrl());
    }

    @Test
    void testSetAppIssueUrl() {
        setFieldsAndInit("Unknown", "Unknown", "Unknown", "", "", "https://github.com/example/issues");
        assertEquals("https://github.com/example/issues", metadata.getIssueUrl());
    }

    @Test
    void testSetAppIssueUrl_NullValue() {
        setFieldsAndInit("Unknown", "Unknown", "Unknown", "", "", null);
        assertEquals("", metadata.getIssueUrl());
    }

    @Test
    void testGetMetadata_NotNull() {
        setFieldsAndInit("Unknown", "Unknown", "Unknown", "", "", "");
        assertNotNull(configLoader.getMetadata());
    }

    @Test
    void testCompleteConfiguration() {
        setFieldsAndInit(
                "My App",
                "2.0.0",
                "Alice, Bob",
                "https://myapp.com",
                "https://myapp.com/help",
                "https://github.com/myapp/issues"
        );

        assertEquals("My App", metadata.getName());
        assertEquals("2.0.0", metadata.getVersion());
        assertArrayEquals(new String[]{"Alice", "Bob"}, metadata.getAuthors());
        assertEquals("https://myapp.com", metadata.getHomepage());
        assertEquals("https://myapp.com/help", metadata.getSupportUrl());
        assertEquals("https://github.com/myapp/issues", metadata.getIssueUrl());
    }

    @Test
    void testInitWithNullMetadata() {
        ConfigLoader loader = new ConfigLoader(null);
        ReflectionTestUtils.setField(loader, "appName", "Unknown Application");
        ReflectionTestUtils.setField(loader, "appVersion", "Unknown Version");
        ReflectionTestUtils.setField(loader, "appAuthors", "Unknown Authors");
        ReflectionTestUtils.setField(loader, "appHomepage", "");
        ReflectionTestUtils.setField(loader, "appSupportUrl", "");
        ReflectionTestUtils.setField(loader, "appIssueUrl", "");

        assertDoesNotThrow(loader::init);
    }

    @Test
    void testMultipleInitCalls() {
        setFieldsAndInit("First Name", "Unknown", "Unknown", "", "", "");
        assertEquals("First Name", metadata.getName());

        ReflectionTestUtils.setField(configLoader, "appName", "Second Name");
        configLoader.init();
        assertEquals("Second Name", metadata.getName());
    }

    @Test
    void testEmptyConfigurationValues() {
        setFieldsAndInit("", "", "", "", "", "");

        assertEquals("", metadata.getName());
        assertEquals("", metadata.getVersion());
        assertEquals("", metadata.getHomepage());
        assertEquals("", metadata.getSupportUrl());
        assertEquals("", metadata.getIssueUrl());
    }

    @Test
    void testFallbackConfig_SystemProperties() {
        System.setProperty("app.name", "System Prop App");
        System.setProperty("app.version", "3.0.0");

        ConfigLoader loader = new ConfigLoader(null);
        ReflectionTestUtils.setField(loader, "appName", null);
        ReflectionTestUtils.setField(loader, "appVersion", null);
        ReflectionTestUtils.setField(loader, "appAuthors", null);
        ReflectionTestUtils.setField(loader, "appHomepage", null);
        ReflectionTestUtils.setField(loader, "appSupportUrl", null);
        ReflectionTestUtils.setField(loader, "appIssueUrl", null);

        assertDoesNotThrow(loader::init);

        System.clearProperty("app.name");
        System.clearProperty("app.version");
    }
}