package de.innologic.templateservice.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingTemplateEventPublisher implements TemplateEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingTemplateEventPublisher.class);

    @Override
    public void publish(TemplateDomainEvent event) {
        log.info(
                "Template event published: type={} tenantId={} scope={} templateId={} versionNo={} actorSub={} occurredAt={}",
                event.eventType(),
                event.tenantId(),
                event.scope(),
                event.templateId(),
                event.versionNo(),
                event.actorSub(),
                event.occurredAt()
        );
    }
}
