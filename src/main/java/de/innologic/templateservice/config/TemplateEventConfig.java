package de.innologic.templateservice.config;

import de.innologic.templateservice.events.LoggingTemplateEventPublisher;
import de.innologic.templateservice.events.NoopTemplateEventPublisher;
import de.innologic.templateservice.events.TemplateEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemplateEventConfig {

    @Bean
    @ConditionalOnProperty(name = "template.events.enabled", havingValue = "true")
    TemplateEventPublisher loggingTemplateEventPublisher() {
        return new LoggingTemplateEventPublisher();
    }

    @Bean
    @ConditionalOnMissingBean(TemplateEventPublisher.class)
    TemplateEventPublisher noopTemplateEventPublisher() {
        return new NoopTemplateEventPublisher();
    }
}
