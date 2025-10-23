package io.pants.humanpanic.unitTest.config;

import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.ConfigLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for ConfigLoader using Mockito
 */
@ExtendWith(MockitoExtension.class)
class ConfigLoaderTest {

    @Mock
    private ConfigLoader configLoader;

    @Test
    void testDefaultValues() {
        // When no configuration is set
        configLoader.init();

        AppMetadata metadata = configLoader.getMetadata();

        // Then defaults should be used
        assertNotNull(metadata);
        assertNotNull(metadata.getName());
        assertNotNull(metadata.getVersion());
    }

    @Test
    void testSetAppName() {
        // Given
        String expectedName = "Test Application";

        // When
        configLoader.setAppName(expectedName);
        configLoader.init();

        // Then
        assertEquals(expectedName, configLoader.getMetadata().getName());
    }

    @Test
    void testSetAppVersion() {
        // Given
        String expectedVersion = "1.2.3";

        // When
        configLoader.setAppVersion(expectedVersion);
        configLoader.init();

        // Then
        assertEquals(expectedVersion, configLoader.getMetadata().getVersion());
    }

    @Test
    void testSetAppAuthors_SingleAuthor() {
        // Given
        String authors = "John Doe";

        // When
        configLoader.setAppAuthors(authors);
        configLoader.init();

        // Then
        String[] result = configLoader.getMetadata().getAuthors();
        assertEquals(1, result.length);
        assertEquals("John Doe", result[0]);
    }

    @Test
    void testSetAppAuthors_MultipleAuthors() {
        // Given
        String authors = "John Doe, Jane Smith, Bob Wilson";

        // When
        configLoader.setAppAuthors(authors);
        configLoader.init();

        // Then
        String[] result = configLoader.getMetadata().getAuthors();
        assertEquals(3, result.length);
        assertEquals("John Doe", result[0]);
        assertEquals("Jane Smith", result[1]);
        assertEquals("Bob Wilson", result[2]);
    }

    @Test
    void testSetAppAuthors_EmptyString() {
        // Given
        String authors = "";

        // When
        configLoader.setAppAuthors(authors);
        configLoader.init();

        // Then
        String[] result = configLoader.getMetadata().getAuthors();
        assertEquals(1, result.length);
        assertEquals("Unknown", result[0]);
    }

    @Test
    void testSetAppHomepage() {
        // Given
        String homepage = "https://example.com";

        // When
        configLoader.setAppHomepage(homepage);
        configLoader.init();

        // Then
        assertEquals(homepage, configLoader.getMetadata().getHomepage());
    }

    @Test
    void testSetAppSupportUrl() {
        // Given
        String supportUrl = "https://example.com/support";

        // When
        configLoader.setAppSupportUrl(supportUrl);
        configLoader.init();

        // Then
        assertEquals(supportUrl, configLoader.getMetadata().getSupportUrl());
    }

    @Test
    void testSetAppIssueUrl() {
        // Given
        String issueUrl = "https://github.com/example/issues";

        // When
        configLoader.setAppIssueUrl(issueUrl);
        configLoader.init();

        // Then
        assertEquals(issueUrl, configLoader.getMetadata().getIssueUrl());
    }

    @Test
    void testGetMetadata_NotNull() {
        // When
        configLoader.init();
        AppMetadata metadata = configLoader.getMetadata();

        // Then
        assertNotNull(metadata);
    }

    @Test
    void testCompleteConfiguration() {
        // Given
        configLoader.setAppName("My App");
        configLoader.setAppVersion("2.0.0");
        configLoader.setAppAuthors("Alice, Bob");
        configLoader.setAppHomepage("https://myapp.com");
        configLoader.setAppSupportUrl("https://myapp.com/help");
        configLoader.setAppIssueUrl("https://github.com/myapp/issues");

        // When
        configLoader.init();
        AppMetadata metadata = configLoader.getMetadata();

        // Then
        assertEquals("My App", metadata.getName());
        assertEquals("2.0.0", metadata.getVersion());
        assertEquals(2, metadata.getAuthors().length);
        assertEquals("https://myapp.com", metadata.getHomepage());
        assertEquals("https://myapp.com/help", metadata.getSupportUrl());
        assertEquals("https://github.com/myapp/issues", metadata.getIssueUrl());
    }
}
