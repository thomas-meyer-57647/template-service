package de.innologic.templateservice.api.dto;

import de.innologic.templateservice.domain.enums.TemplateScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "TemplateFamilyResponse", description = "Antwortobjekt einer Template-Familie.")
public record TemplateFamilyResponse(
    @Schema(description = "Primärschlüssel der Template-Familie.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
    UUID templateId,
    @Schema(description = "Scope der Template-Familie.", example = "TENANT")
    TemplateScope scope,
    @Schema(description = "Owner-Tenant-ID.", example = "tenant-acme")
    String ownerTenantId,
    @Schema(description = "Fachlicher Schlüssel.", example = "invoice.reminder")
    String templateKey,
    @Schema(description = "Kanal.", example = "EMAIL")
    String channel,
    @Schema(description = "Locale.", example = "de-DE")
    String locale,
    @Schema(description = "Kategorie.", example = "BILLING")
    String category,
    @Schema(description = "Aktive freigegebene Versionsnummer.", example = "3")
    Integer activeApprovedVersion,
    @Schema(description = "Anlagezeitpunkt in UTC.", example = "2026-02-15T14:58:30Z")
    Instant createdAt,
    @Schema(description = "Angelegt von.", example = "system-admin")
    String createdBy,
    @Schema(description = "Letzte Aktualisierung in UTC.", example = "2026-02-15T15:01:00Z")
    Instant updatedAt,
    @Schema(description = "Zuletzt geändert von.", example = "system-admin")
    String updatedBy
) {
}
