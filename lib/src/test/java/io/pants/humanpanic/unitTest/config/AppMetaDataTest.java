package io.pants.humanpanic.unitTest.config;

import io.pants.humanpanic.config.AppMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for AppMetadata
 */
@ExtendWith(MockitoExtension.class)
public class AppMetaDataTest {
    private AppMetadata metadata;

    @BeforeEach
    void setUp() {
        metadata = new AppMetadata();
    }

    @Test
    void testDefaultValues() {
        assertEquals("Unknown Application", metadata.getName());
        assertEquals("Unknown Version", metadata.getVersion());
        assertArrayEquals(new String[]{"Unknown Authors"}, metadata.getAuthors());
        assertEquals("Unknown Homepage", metadata.getHomepage());
        assertEquals("Unknown Support-URL", metadata.getSupportUrl());
        assertEquals("Unknown Issue_URL", metadata.getIssueUrl());
    }

    @Test
    void testSetName() {
        metadata.setName("Test App");
        assertEquals("Test App", metadata.getName());
    }

    @Test
    void testSetVersion() {
        metadata.setVersion("1.0.0");
        assertEquals("1.0.0", metadata.getVersion());
    }

    @Test
    void testSetAuthors() {
        String[] authors = {"Author1", "Author2"};
        metadata.setAuthors(authors);
        assertArrayEquals(authors, metadata.getAuthors());
    }

    @Test
    void testSetHomepage() {
        metadata.setHomepage("https://example.com");
        assertEquals("https://example.com", metadata.getHomepage());
    }

    @Test
    void testSetSupportUrl() {
        metadata.setSupportUrl("https://support.example.com");
        assertEquals("https://support.example.com", metadata.getSupportUrl());
    }

    @Test
    void testSetIssueUrl() {
        metadata.setIssueUrl("https://github.com/example/issues");
        assertEquals("https://github.com/example/issues", metadata.getIssueUrl());
    }

    @Test
    void testSetNullValues() {
        metadata.setName(null);
        metadata.setVersion(null);
        metadata.setAuthors(null);
        metadata.setHomepage(null);
        metadata.setSupportUrl(null);
        metadata.setIssueUrl(null);

        assertNull(metadata.getName());
        assertNull(metadata.getVersion());
        assertNull(metadata.getAuthors());
        assertNull(metadata.getHomepage());
        assertNull(metadata.getSupportUrl());
        assertNull(metadata.getIssueUrl());
    }

    @Test
    void testSetEmptyStrings() {
        metadata.setName("");
        metadata.setVersion("");
        metadata.setHomepage("");
        metadata.setSupportUrl("");
        metadata.setIssueUrl("");

        assertEquals("", metadata.getName());
        assertEquals("", metadata.getVersion());
        assertEquals("", metadata.getHomepage());
        assertEquals("", metadata.getSupportUrl());
        assertEquals("", metadata.getIssueUrl());
    }

    @Test
    void testSetEmptyAuthorsArray() {
        String[] emptyAuthors = {};
        metadata.setAuthors(emptyAuthors);
        assertArrayEquals(emptyAuthors, metadata.getAuthors());
    }

    @Test
    void testMultipleAuthors() {
        String[] authors = {"John Doe", "Jane Smith", "Bob Wilson"};
        metadata.setAuthors(authors);
        assertEquals(3, metadata.getAuthors().length);
        assertEquals("John Doe", metadata.getAuthors()[0]);
        assertEquals("Jane Smith", metadata.getAuthors()[1]);
        assertEquals("Bob Wilson", metadata.getAuthors()[2]);
    }

    @Test
    void testCompleteMetadata() {
        metadata.setName("My Application");
        metadata.setVersion("2.0.0");
        metadata.setAuthors(new String[]{"Developer1", "Developer2"});
        metadata.setHomepage("https://myapp.com");
        metadata.setSupportUrl("https://support.myapp.com");
        metadata.setIssueUrl("https://github.com/myapp/issues");

        assertEquals("My Application", metadata.getName());
        assertEquals("2.0.0", metadata.getVersion());
        assertEquals(2, metadata.getAuthors().length);
        assertEquals("https://myapp.com", metadata.getHomepage());
        assertEquals("https://support.myapp.com", metadata.getSupportUrl());
        assertEquals("https://github.com/myapp/issues", metadata.getIssueUrl());
    }
}
