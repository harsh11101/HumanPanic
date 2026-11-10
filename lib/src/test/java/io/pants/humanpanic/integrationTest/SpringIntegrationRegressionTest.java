package io.pants.humanpanic.integrationTest;

import io.pants.humanpanic.HumanPanic;
import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.HumanPanicConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring wiring scenarios that the original suite did not cover.
 */
class SpringIntegrationRegressionTest {

    public interface Greeter {
        String greet(String name);
    }

    public static class FailingGreeter implements Greeter {
        @HumanPanic(message = "Greeting failed.")
        @Override
        public String greet(String name) {
            throw new IllegalStateException("no greeting for " + name);
        }
    }

    @Configuration
    @Import(HumanPanicConfiguration.class)
    static class PlainSpringConfig {
        @Bean
        Greeter greeter() {
            return new FailingGreeter();
        }
    }

    @Configuration
    @Import(HumanPanicConfiguration.class)
    static class BootConfig {
        @Bean
        Greeter greeter() {
            return new FailingGreeter();
        }

        // Beans with the generic names the library used to register (A5). Spring Boot forbids
        // bean overriding, so these would have failed startup or been silently replaced.
        @Bean
        String appMetadata() {
            return "consumer bean";
        }

        @Bean
        String crashReporter() {
            return "consumer bean";
        }

        @Bean
        String configLoader() {
            return "consumer bean";
        }
    }

    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private PrintStream originalErr;

    @BeforeEach
    void captureErr() {
        originalErr = System.err;
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restore() throws IOException {
        System.setErr(originalErr);
        Path reports = Path.of("crash-reports");
        if (Files.exists(reports)) {
            try (Stream<Path> files = Files.walk(reports)) {
                files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    @Test
    void interfaceBeanBehindJdkProxyIsHandled() {
        // Regression (B1): with plain @EnableAspectJAutoProxy, interface beans get JDK proxies and
        // the aspect used to fail with an NPE because it read the annotation from the interface.
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(PlainSpringConfig.class)) {
            Greeter greeter = ctx.getBean(Greeter.class);
            assertTrue(AopUtils.isJdkDynamicProxy(greeter), "precondition: JDK proxy");

            assertNull(greeter.greet("Ada"));

            String out = err.toString(StandardCharsets.UTF_8);
            assertTrue(out.contains("Greeting failed."), out);
            assertTrue(out.contains("We have generated a report file at"), out);
        }
    }

    @Test
    void reportNamesTheImplementationClassNotTheInterface() throws IOException {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(PlainSpringConfig.class)) {
            ctx.getBean(Greeter.class).greet("Ada");
        }
        try (Stream<Path> files = Files.list(Path.of("crash-reports"))) {
            Path report = files.findFirst().orElseThrow();
            String methodClass = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(report.toFile()).at("/method/class").asText();
            assertEquals(FailingGreeter.class.getName(), methodClass);
        }
    }

    @Test
    void springBootContextWithPropertiesAndCollidingBeanNames() {
        try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(BootConfig.class)
                .web(WebApplicationType.NONE)
                .properties("app.name=Boot Tool", "app.authors=Ada, Linus", "spring.main.banner-mode=off")
                .logStartupInfo(false)
                .run()) {

            assertEquals("Boot Tool", ctx.getBean(AppMetadata.class).getName());
            assertEquals("consumer bean", ctx.getBean("appMetadata"));

            assertNull(ctx.getBean(Greeter.class).greet("Linus"));

            String out = err.toString(StandardCharsets.UTF_8);
            assertTrue(out.contains("Boot Tool had a problem and crashed"), out);
            assertTrue(out.contains("Ada\n") && out.contains("Linus\n"), out);
        }
    }

    @Test
    void libraryDoesNotComponentScan() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(HumanPanicConfiguration.class)) {
            for (String name : ctx.getBeanDefinitionNames()) {
                Object bean = ctx.getBean(name);
                if (bean.getClass().getName().startsWith("io.pants.humanpanic")) {
                    assertTrue(name.startsWith("humanPanic") || name.contains("HumanPanicConfiguration"),
                            "unexpected library bean name: " + name);
                }
            }
            assertEquals(0, ctx.getBeanNamesForType(io.pants.humanpanic.config.NonSpringConfigLoader.class).length);
        }
    }
}
