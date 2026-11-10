package io.pants.humanpanic;

import io.pants.humanpanic.config.AppMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PanicHandlerTest {

    @TempDir
    Path dir;

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private Thread.UncaughtExceptionHandler originalDefault;

    @BeforeEach
    void rememberDefaultHandler() {
        originalDefault = Thread.getDefaultUncaughtExceptionHandler();
    }

    @AfterEach
    void restoreDefaultHandler() {
        Thread.setDefaultUncaughtExceptionHandler(originalDefault);
    }

    private String output() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private PanicHandler handler(AppMetadata metadata) {
        return PanicHandler.builder()
                .metadata(metadata)
                .reportDirectory(dir)
                .output(new PrintStream(buffer, true, StandardCharsets.UTF_8))
                .build();
    }

    private static AppMetadata app(String name) {
        AppMetadata m = new AppMetadata();
        m.setName(name);
        return m;
    }

    public interface Command {
        @HumanPanic(message = "Could not run the command.")
        int run();
    }

    @Test
    void handleWritesReportAndMessage() throws IOException {
        Optional<Path> report = handler(app("csvtool")).handle(new IllegalStateException("bad row"));

        assertTrue(report.isPresent());
        assertTrue(Files.readString(report.get()).contains("java.lang.IllegalStateException: bad row"));
        assertTrue(output().contains("csvtool had a problem and crashed"));
        assertTrue(output().contains(report.get().toString()));
    }

    @Test
    void handleReturnsEmptyWhenReportCannotBeWritten() throws IOException {
        Path file = Files.writeString(dir.resolve("file"), "x");
        PanicHandler handler = PanicHandler.builder()
                .reportDirectory(file)
                .output(new PrintStream(buffer, true, StandardCharsets.UTF_8))
                .build();

        assertEquals(Optional.empty(), handler.handle(new RuntimeException()));
        assertTrue(output().contains("could not be written"));
    }

    @Test
    void metadataIsCopiedAtBuildTime() {
        AppMetadata m = app("before");
        PanicHandler handler = handler(m);
        m.setName("after");

        handler.handle(new RuntimeException());

        assertTrue(output().contains("before had a problem"));
    }

    @Test
    void uncaughtExceptionInThreadIsHandled() throws Exception {
        Thread t = new Thread(() -> {
            throw new IllegalArgumentException("worker died");
        }, "worker-1");
        t.setUncaughtExceptionHandler(handler(app("svc")).asUncaughtExceptionHandler());

        t.start();
        t.join();

        assertTrue(output().contains("svc had a problem and crashed"));
        try (var files = Files.list(dir)) {
            Path report = files.findFirst().orElseThrow();
            String json = Files.readString(report);
            assertTrue(json.contains("worker died"));
            assertTrue(json.contains("\"thread\" : \"worker-1\""));
        }
    }

    @Test
    void installSetsTheDefaultHandler() throws Exception {
        handler(app("global")).install();

        Thread t = new Thread(() -> {
            throw new RuntimeException("unhandled");
        });
        t.start();
        t.join();

        assertNotNull(Thread.getDefaultUncaughtExceptionHandler());
        assertTrue(output().contains("global had a problem"));
    }

    @Test
    void wrapAppliesAnnotations() {
        Command cmd = handler(app("cli")).wrap(() -> {
            throw new IllegalStateException("nope");
        }, Command.class);

        assertEquals(0, cmd.run());
        assertTrue(output().contains("Could not run the command."));
    }

    @Test
    void builderDefaults() {
        PanicHandler handler = PanicHandler.builder().build();

        assertEquals(Path.of("crash-reports"), handler.getReportDirectory());
    }

    @Test
    void builderRejectsNulls() {
        PanicHandler.Builder b = PanicHandler.builder();
        assertThrows(NullPointerException.class, () -> b.metadata(null));
        assertThrows(NullPointerException.class, () -> b.reportDirectory(null));
        assertThrows(NullPointerException.class, () -> b.output(null));
        assertThrows(NullPointerException.class, () -> handler(app("x")).handle(null));
    }
}
