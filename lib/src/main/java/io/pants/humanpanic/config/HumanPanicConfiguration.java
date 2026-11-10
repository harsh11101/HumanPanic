package io.pants.humanpanic.config;

import io.pants.humanpanic.interceptor.HumanPanicAspect;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * <b>Spring integration.</b> Import this configuration to activate {@code @HumanPanic} on Spring
 * beans:
 *
 * <pre>{@code
 * @Configuration
 * @Import(HumanPanicConfiguration.class)
 * class AppConfig { }
 * }</pre>
 *
 * <p>Your application must have {@code spring-context} (6.x) and {@code org.aspectj:aspectjweaver}
 * on its classpath. HumanPanic does not bring them in. Application metadata is read from the
 * {@code app.*} properties (see {@link ConfigLoader}), and messages are written to
 * {@link System#err}.
 *
 * <p>All beans are explicitly declared, and their names start with {@code humanPanic} so they do
 * not clash with your own beans. No component scanning is performed.
 */
@Configuration
@EnableAspectJAutoProxy
public class HumanPanicConfiguration {

    /** Creates the configuration (instantiated by Spring). */
    public HumanPanicConfiguration() {
    }

    /**
     * The application metadata, populated by {@link #humanPanicConfigLoader()}.
     *
     * @return the shared metadata instance
     */
    @Bean
    public AppMetadata humanPanicAppMetadata() {
        return new AppMetadata();
    }

    /**
     * Binds {@code app.*} properties into {@link #humanPanicAppMetadata()}.
     *
     * @return the loader
     */
    @Bean
    public ConfigLoader humanPanicConfigLoader() {
        return new ConfigLoader(humanPanicAppMetadata());
    }

    /**
     * The crash report writer.
     *
     * @return the reporter
     */
    @Bean
    public CrashReporter humanPanicCrashReporter() {
        return new CrashReporter(humanPanicConfigLoader());
    }

    /**
     * The user message writer.
     *
     * @return the notifier
     */
    @Bean
    public UserNotifier humanPanicUserNotifier() {
        return new UserNotifier(humanPanicConfigLoader());
    }

    /**
     * The aspect that intercepts {@code @HumanPanic} methods.
     *
     * @return the aspect
     */
    @Bean
    public HumanPanicAspect humanPanicAspect() {
        return new HumanPanicAspect(humanPanicCrashReporter(), humanPanicUserNotifier());
    }
}
