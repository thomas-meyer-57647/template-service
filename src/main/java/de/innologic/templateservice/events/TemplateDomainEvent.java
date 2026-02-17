package de.innologic.templateservice.events;

import java.time.Instant;
import java.util.UUID;

public interface TemplateDomainEvent {

    String eventType();

    String tenantId();

    String scope();

    UUID templateId();

    Integer versionNo();

    String actorSub();

    Instant occurredAt();
}
