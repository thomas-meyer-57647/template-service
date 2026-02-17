package de.innologic.templateservice.events;

import java.time.Instant;
import java.util.UUID;

public record TemplateVersionDeprecatedEvent(
        String tenantId,
        String scope,
        UUID templateId,
        Integer versionNo,
        String actorSub,
        Instant occurredAt
) implements TemplateDomainEvent {

    @Override
    public String eventType() {
        return "TemplateVersionDeprecated";
    }
}
