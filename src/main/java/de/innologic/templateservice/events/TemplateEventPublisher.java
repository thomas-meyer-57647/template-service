package de.innologic.templateservice.events;

public interface TemplateEventPublisher {

    void publish(TemplateDomainEvent event);
}
