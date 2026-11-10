package io.pants.humanpanic.reporter;

import io.pants.humanpanic.config.AppMetadata;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class UserNotifierOutputTest {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);

    private String output() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void defaultMetadataProducesNoPlaceholderSections() {
        // Regression (B5): default metadata used to print "Get help at: Unknown Support-URL" etc.
        new UserNotifier(new AppMetadata(), out).notifyWithReport("", "/tmp/r.json");

        String text = output();
        assertTrue(text.contains("Unknown Application had a problem and crashed"));
        assertFalse(text.contains("Open an issue at"));
        assertFalse(text.contains("Get help at"));
        assertFalse(text.contains("Contact the authors"));
        assertFalse(text.contains("Unknown Support-URL"));
    }

    @Test
    void toleratesNullMetadataFields() {
        AppMetadata m = new AppMetadata();
        m.setName(null);
        m.setAuthors(null);
        m.setIssueUrl(null);
        m.setSupportUrl(null);

        UserNotifier notifier = new UserNotifier(m, out);

        assertDoesNotThrow(() -> notifier.notifyWithReport(null, null));
        assertDoesNotThrow(() -> notifier.notify(null, null));
        assertTrue(output().contains(AppMetadata.DEFAULT_NAME));
    }

    @Test
    void missingReportPathIsExplained() {
        new UserNotifier(new AppMetadata(), out).notifyWithReport("", null);

        assertTrue(output().contains("the crash report could not be written"));
        assertFalse(output().contains("We have generated a report file at"));
    }

    @Test
    void printStackTraceWritesToConfiguredStream() {
        new UserNotifier(new AppMetadata(), out).printStackTrace(new IllegalStateException("traced"));

        assertTrue(output().contains("java.lang.IllegalStateException: traced"));
        assertTrue(output().contains("\tat "));
    }

    @Test
    void writesToSystemErrLazilyByDefault() {
        PrintStream original = System.err;
        try {
            UserNotifier notifier = new UserNotifier(new AppMetadata()); // created before redirect
            System.setErr(out);
            notifier.notify("lazy", new RuntimeException());
        } finally {
            System.setErr(original);
        }
        assertTrue(output().contains("lazy"));
    }

    @Test
    void concurrentMessagesDoNotInterleave() throws Exception {
        UserNotifier notifier = new UserNotifier(new AppMetadata(), out);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 200; i++) {
            pool.submit(() -> notifier.notifyWithReport("msg", "/r.json"));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        String[] blocks = output().split("\nWell, this is embarrassing\\.\n", -1);
        assertEquals(201, blocks.length); // leading empty chunk + 200 messages
        for (int i = 1; i < blocks.length; i++) {
            assertTrue(blocks[i].endsWith("Thank you kindly!\n"), "message " + i + " was interleaved");
        }
    }

    @Test
    void constructorsRejectNulls() {
        assertThrows(NullPointerException.class, () -> new UserNotifier((AppMetadata) null));
        assertThrows(NullPointerException.class, () -> new UserNotifier(null, out));
        assertThrows(NullPointerException.class, () -> new UserNotifier(new AppMetadata(), null));
    }
}
