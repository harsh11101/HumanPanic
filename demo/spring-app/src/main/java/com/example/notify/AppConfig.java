package com.example.notify;

import io.pants.humanpanic.config.HumanPanicConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/** Plain Spring (no Boot). Importing HumanPanicConfiguration is the whole integration. */
@Configuration
@Import(HumanPanicConfiguration.class)
@PropertySource("classpath:application.properties")
@ComponentScan("com.example.notify")
public class AppConfig {
}
