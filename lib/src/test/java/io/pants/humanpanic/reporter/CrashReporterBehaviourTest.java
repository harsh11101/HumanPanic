package io.pants.humanpanic.reporter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.model.CrashReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CrashReporterBehaviourTest {

    @TempDir
    Path dir;

    private final ObjectMapper jackson = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    private static AppMetadata metadata() {
        AppMetadata m = new AppMetadata();
        m.setName("Reporter Test");
        m.setVersion("9.9.9");
        m.setAuthors(new String[]{"Ada", "Linus"});
        m.setIssueUrl("https://example.com/issues");
        return m;
    }

    private CrashReporter reporter() {
        return new CrashReporter(metadata(), dir.resolve("reports"));
    }

    private JsonNode read(String path) throws IOException {
        return jackson.readTree(Files.readString(Path.of(path)));
    }

    @Test
    void writesToConfiguredDirectoryCreatingIt() {
        String path = reporter().createReport(new RuntimeException("x"), null);

        assertNotNull(path);
        assertEquals(dir.resolve("reports").toAbsolutePath(), Path.of(path).getParent());
        assertTrue(Path.of(path).getFileName().toString().matches(
                "crash-\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{3}-[0-9a-f]{6}\\.json"));
    }

    @Test
    void concurrentFailuresProduceDistinctFiles() throws Exception {
        // Regression (B3): reports used to be named by the second and overwrite each other.
        CrashReporter reporter = reporter();
        int threads = 32;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        Set<String> paths = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < threads; i++) {
            int n = i;
            pool.submit(() -> {
                start.await();
                String path = reporter.createReport(new RuntimeException("failure " + n), null);
                assertNotNull(path, "report " + n + " was not written");
                paths.add(path);
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(threads, paths.size());
        try (Stream<Path> files = Files.list(dir.resolve("reports"))) {
            assertEquals(threads, files.count());
        }
    }

    @Test
    void recordsTheCauseChain() throws Exception {
        // Regression (B6): the root cause used to be dropped.
        Exception root = new IOException("disk full");
        Exception middle = new IllegalStateException("cannot save", root);
        RuntimeException top = new RuntimeException("command failed", middle);

        JsonNode report = read(reporter().createReport(top, null));

        assertEquals("java.lang.RuntimeException: command failed", report.get("cause").asText());
        assertEquals(2, report.get("caused_by").size());
        assertEquals("java.lang.IllegalStateException: cannot save", report.at("/caused_by/0/cause").asText());
        assertEquals("java.io.IOException: disk full", report.at("/caused_by/1/cause").asText());
        assertTrue(report.at("/caused_by/1/backtrace").isArray());
    }

    @Test
    void cyclicCauseChainTerminates() throws Exception {
        Exception a = new Exception("a");
        Exception b = new Exception("b", a);
        a.initCause(b);

        JsonNode report = read(reporter().createReport(a, null));

        assertEquals(1, report.get("caused_by").size());
    }

    @Test
    void toleratesNullMetadataFields() throws Exception {
        // Regression (B4): null metadata used to throw an NPE out of the reporter.
        AppMetadata m = new AppMetadata();
        m.setName(null);
        m.setVersion(null);
        m.setAuthors(null);
        m.setHomepage(null);
        m.setSupportUrl(null);
        m.setIssueUrl(null);

        String path = new CrashReporter(m, dir).createReport(new RuntimeException(), null);

        JsonNode report = read(path);
        assertEquals(AppMetadata.DEFAULT_NAME, report.get("name").asText());
        assertEquals(AppMetadata.DEFAULT_VERSION, report.get("version").asText());
        assertFalse(report.get("application_info").has("authors"));
    }

    @Test
    void returnsNullWhenDirectoryCannotBeCreated() throws IOException {
        Path file = Files.writeString(dir.resolve("not-a-directory"), "x");

        assertNull(new CrashReporter(metadata(), file).createReport(new RuntimeException(), null));
    }

    @Test
    void returnsNullForNullThrowable() {
        assertNull(reporter().createReport(null, null));
    }

    @Test
    void handlesExceptionWithoutStackTrace() throws Exception {
        RuntimeException noTrace = new RuntimeException("no trace", null, false, false) { };

        JsonNode report = read(reporter().createReport(noTrace, null));

        assertEquals(0, report.get("backtrace").size());
    }

    @Test
    void includesTimestampThreadAndMethod() throws Exception {
        String path = reporter().createReport(new RuntimeException(),
                CrashReporterBehaviourTest.class.getDeclaredMethod("reporter"));

        JsonNode report = read(path);
        assertDoesNotThrow(() -> OffsetDateTime.parse(report.get("timestamp").asText()));
        assertEquals(Thread.currentThread().getName(), report.get("thread").asText());
        assertEquals(CrashReporterBehaviourTest.class.getName(), report.at("/method/class").asText());
        assertEquals("reporter", report.at("/method/method").asText());
        assertEquals("Ada, Linus", report.at("/application_info/authors").asText());
        assertEquals("https://example.com/issues", report.at("/application_info/issues").asText());
        assertFalse(report.get("application_info").has("homepage"));
    }

    @Test
    void jsonFieldNamesMatchTheModelAnnotations() throws Exception {
        // The internal JSON writer and CrashReport's @JsonProperty names must agree, so that
        // consumers can deserialise reports with Jackson. FAIL_ON_UNKNOWN_PROPERTIES is on.
        String path = reporter().createReport(
                new RuntimeException("outer", new IOException("inner")),
                CrashReporterBehaviourTest.class.getDeclaredMethod("reporter"));

        CrashReport report = jackson.readValue(Path.of(path).toFile(), CrashReport.class);

        assertEquals("Reporter Test", report.getName());
        assertEquals("9.9.9", report.getVersion());
        assertEquals("reporter", report.getMethod().getMethodName());
        assertFalse(report.getBacktrace().isEmpty());
        assertEquals(List.of("java.io.IOException: inner"),
                report.getCausedBy().stream().map(CrashReport.Cause::getCause).toList());
        assertTrue(report.getSystemInfo().getProcessors() > 0);
    }

    @Test
    void metadataIsReadAtReportTime() throws Exception {
        AppMetadata m = metadata();
        CrashReporter reporter = new CrashReporter(m, dir);
        m.setName("Renamed Later");

        assertEquals("Renamed Later", read(reporter.createReport(new RuntimeException(), null)).get("name").asText());
    }

    @Test
    void constructorsRejectNulls() {
        assertThrows(NullPointerException.class, () -> new CrashReporter(null, dir));
        assertThrows(NullPointerException.class, () -> new CrashReporter(metadata(), null));
        assertThrows(NullPointerException.class, () -> new CrashReporter((io.pants.humanpanic.config.ConfigLoader) null));
    }
}
