package io.pants.humanpanic.integrationTest;

import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.PanicHandler;
import io.pants.humanpanic.config.AppMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs scenarios in a separate JVM so that process exit codes, {@code System.exit} and the
 * uncaught-exception path of {@code main} are observed for real.
 */
class ForkedJvmIntegrationTest {

    @TempDir
    Path workDir;

    /** Entry point executed in the child JVM. */
    public static final class Child {
        public interface Job {
            @HumanPanic(exitCode = 3, message = "Job failed; exiting.")
            void run();
        }

        public static void main(String[] args) {
            AppMetadata app = new AppMetadata();
            app.setName("ForkedApp");
            PanicHandler handler = PanicHandler.builder()
                    .metadata(app)
                    .reportDirectory(Paths.get(args[1]))
                    .build()
                    .install();
            switch (args[0]) {
                case "uncaught":
                    throw new IllegalStateException("crash in main");
                case "exit-code":
                    Job job = handler.wrap(() -> {
                        throw new IllegalArgumentException("bad job");
                    }, Job.class);
                    job.run();
                    System.out.println("UNREACHABLE");
                    return;
                default:
                    throw new IllegalArgumentException(args[0]);
            }
        }
    }

    private Result run(String scenario) throws IOException, InterruptedException {
        Path reports = workDir.resolve("reports");
        String java = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        Process p = new ProcessBuilder(List.of(java, "-cp", System.getProperty("java.class.path"),
                Child.class.getName(), scenario, reports.toString()))
                .redirectErrorStream(false)
                .start();
        String stdout = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(p.waitFor(60, TimeUnit.SECONDS), "child JVM timed out");
        return new Result(p.exitValue(), stdout, stderr, reports);
    }

    private record Result(int exitCode, String stdout, String stderr, Path reports) {
        long reportCount() throws IOException {
            if (!Files.isDirectory(reports)) {
                return 0;
            }
            try (Stream<Path> s = Files.list(reports)) {
                return s.count();
            }
        }
    }

    @Test
    void uncaughtExceptionInMainShowsFriendlyMessageAndExitsWithOne() throws Exception {
        Result r = run("uncaught");

        assertEquals(1, r.exitCode(), r.stderr());
        assertTrue(r.stderr().contains("ForkedApp had a problem and crashed"), r.stderr());
        assertFalse(r.stderr().contains("\tat "), "no raw stack trace for the user:\n" + r.stderr());
        assertEquals(1, r.reportCount());
    }

    @Test
    void exitCodeAttributeTerminatesJvmWithThatCode() throws Exception {
        Result r = run("exit-code");

        assertEquals(3, r.exitCode(), r.stderr());
        assertTrue(r.stderr().contains("Job failed; exiting."), r.stderr());
        assertFalse(r.stdout().contains("UNREACHABLE"));
        assertEquals(1, r.reportCount());
    }
}
