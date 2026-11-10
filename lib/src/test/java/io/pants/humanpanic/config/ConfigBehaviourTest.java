package io.pants.humanpanic.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ConfigBehaviourTest {

    @TempDir
    Path dir;

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("app.name");
        System.clearProperty("app.issue-url");
    }

    private static final ClassLoader EMPTY = new URLClassLoader(new URL[0], null);

    // --- NonSpringConfigLoader (B7: it used to never load anything) ---

    @Test
    void loadsFromExplicitProperties() {
        Properties p = new Properties();
        p.setProperty("app.name", "csvtool");
        p.setProperty("app.version", "1.2.3");
        p.setProperty("app.authors", " Ada ,, Linus ");
        p.setProperty("app.homepage", "https://h");
        p.setProperty("app.support-url", "https://s");
        p.setProperty("app.issue-url", "https://i");

        NonSpringConfigLoader loader = new NonSpringConfigLoader();
        AppMetadata m = loader.load(p);

        assertSame(m, loader.getMetadata());
        assertEquals("csvtool", m.getName());
        assertEquals("1.2.3", m.getVersion());
        assertArrayEquals(new String[]{"Ada", "Linus"}, m.getAuthors());
        assertEquals("https://h", m.getHomepage());
        assertEquals("https://s", m.getSupportUrl());
        assertEquals("https://i", m.getIssueUrl());
    }

    @Test
    void readsWorkingDirectoryFileAsUtf8() throws IOException {
        Path file = dir.resolve(NonSpringConfigLoader.PROPERTIES_FILE);
        Files.writeString(file, "app.name=Zoë's Tool\napp.authors=Zoë\n", StandardCharsets.UTF_8);

        AppMetadata m = new NonSpringConfigLoader().load(EMPTY, file);

        assertEquals("Zoë's Tool", m.getName());
        assertArrayEquals(new String[]{"Zoë"}, m.getAuthors());
    }

    @Test
    void classpathResourceTakesPrecedenceOverWorkingDirectory() throws IOException {
        Path cpRoot = Files.createDirectories(dir.resolve("cp"));
        Files.writeString(cpRoot.resolve(NonSpringConfigLoader.PROPERTIES_FILE), "app.name=FromClasspath\n");
        Path file = Files.writeString(dir.resolve("wd.properties"), "app.name=FromWorkingDir\n");

        try (URLClassLoader cl = new URLClassLoader(new URL[]{cpRoot.toUri().toURL()}, null)) {
            assertEquals("FromClasspath", new NonSpringConfigLoader().load(cl, file).getName());
        }
    }

    @Test
    void systemPropertiesOverrideFile() throws IOException {
        Path file = Files.writeString(dir.resolve("a.properties"), "app.name=FromFile\napp.version=1\n");
        System.setProperty("app.name", "FromSystem");

        AppMetadata m = new NonSpringConfigLoader().load(EMPTY, file);

        assertEquals("FromSystem", m.getName());
        assertEquals("1", m.getVersion());
    }

    @Test
    void missingFileGivesDefaults() {
        AppMetadata m = new NonSpringConfigLoader().load(EMPTY, dir.resolve("absent.properties"));

        assertEquals(new AppMetadata(), m);
    }

    @Test
    void publicLoadUsesSystemProperties() {
        System.setProperty("app.issue-url", "https://sys/issues");

        assertEquals("https://sys/issues", new NonSpringConfigLoader().load().getIssueUrl());
    }

    @Test
    void doesNotReportTheLibraryManifestAsTheApplication() {
        // Regression (B8): the old fallback read HumanPanic's own jar manifest.
        AppMetadata m = new NonSpringConfigLoader().load(EMPTY, dir.resolve("absent.properties"));

        assertEquals(AppMetadata.DEFAULT_NAME, m.getName());
    }

    // --- AppMetadata ---

    @Test
    void authorsAreDefensivelyCopied() {
        // Regression (B16)
        String[] authors = {"Ada"};
        AppMetadata m = new AppMetadata();
        m.setAuthors(authors);

        authors[0] = "mutated input";
        m.getAuthors()[0] = "mutated output";

        assertArrayEquals(new String[]{"Ada"}, m.getAuthors());
    }

    @Test
    void copyConstructorIsDeepForAuthors() {
        AppMetadata original = new AppMetadata();
        original.setName("x");
        original.setAuthors(new String[]{"a"});

        AppMetadata copy = new AppMetadata(original);
        original.setAuthors(new String[]{"b"});

        assertEquals("x", copy.getName());
        assertArrayEquals(new String[]{"a"}, copy.getAuthors());
        assertThrows(NullPointerException.class, () -> new AppMetadata(null));
    }

    @Test
    void equalsAndHashCodeIncludeAuthors() {
        AppMetadata a = new AppMetadata();
        AppMetadata b = new AppMetadata();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        b.setAuthors(new String[]{"x"});
        assertNotEquals(a, b);
    }

    @Test
    void parseAuthorsTrimsAndDropsBlanks() {
        // Regression (B18)
        assertArrayEquals(new String[]{"a", "b", "c"}, AppMetadata.parseAuthors(" a ,b,, c ,"));
        assertArrayEquals(new String[0], AppMetadata.parseAuthors("  "));
        assertArrayEquals(new String[0], AppMetadata.parseAuthors(null));
    }

    // --- ConfigLoader ---

    @Test
    void configLoaderInitIsIdempotentAndNullSafe() {
        ConfigLoader loader = new ConfigLoader(null);

        loader.afterPropertiesSet();
        loader.init();

        assertNotNull(loader.getMetadata());
        assertEquals(AppMetadata.DEFAULT_NAME, loader.getMetadata().getName());
        assertArrayEquals(new String[0], loader.getMetadata().getAuthors());
    }
}
