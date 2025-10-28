package io.pants.humanpanic.config;

import io.pants.humanpanic.interceptor.HumanPanicAspect;
import io.pants.humanpanic.reporter.CrashReporter;
import io.pants.humanpanic.reporter.UserNotifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Main configuration class for HumanPanic library
 * Enables AspectJ auto-proxying and registers all necessary beans
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = "io.pants.humanpanic")
public class HumanPanicConfiguration {

    /**
     * Required for @Value annotations to work in tests and non-Spring-Boot apps
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public AppMetadata appMetadata() {
        return new AppMetadata();
    }

    @Bean
    public CrashReporter crashReporter(ConfigLoader configLoader) {
        return new CrashReporter(configLoader);
    }

    @Bean
    public UserNotifier userNotifier(ConfigLoader configLoader) {
        return new UserNotifier(configLoader);
    }

    @Bean
    public HumanPanicAspect humanPanicAspect(CrashReporter crashReporter, UserNotifier userNotifier) {
        return new HumanPanicAspect(crashReporter, userNotifier);
    }
}