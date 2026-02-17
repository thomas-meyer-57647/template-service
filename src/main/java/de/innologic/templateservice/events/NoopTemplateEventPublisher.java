package de.innologic.templateservice.events;

public class NoopTemplateEventPublisher implements TemplateEventPublisher {

    @Override
    public void publish(TemplateDomainEvent event) {
        // no-op by default; keeps core write paths unchanged when events are disabled
    }
}
