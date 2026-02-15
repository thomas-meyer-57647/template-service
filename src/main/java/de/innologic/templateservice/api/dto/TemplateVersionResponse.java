package de.innologic.templateservice.api.dto;

import de.innologic.templateservice.domain.enums.RenderTarget;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "TemplateVersionResponse", description = "Antwortobjekt einer Template-Version.")
public record TemplateVersionResponse(
    @Schema(description = "Primärschlüssel der Version.", example = "20811ed5-cbed-4f48-b17b-fba7df80fca1")
    UUID versionId,
    @Schema(description = "Fremdschlüssel zur Template-Familie.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
    UUID templateId,
    @Schema(description = "Versionsnummer innerhalb der Familie.", example = "2")
    Integer versionNo,
    @Schema(description = "Status.", example = "APPROVED")
    TemplateStatus status,
    @Schema(description = "Render-Target.", example = "HTML")
    RenderTarget renderTarget,
    @Schema(description = "Optionales Subject-Template.", example = "Zahlungserinnerung {{invoiceNo}}")
    String subjectTpl,
    @Schema(description = "Body-Template.", example = "<p>Hallo {{customerName}}</p>")
    String bodyTpl,
    @Schema(description = "Placeholder als JSON/String-Liste.", example = "[\"customerName\",\"invoiceNo\"]")
    String placeholders,
    @Schema(description = "Anlagezeitpunkt in UTC.", example = "2026-02-15T14:58:30Z")
    Instant createdAt,
    @Schema(description = "Angelegt von.", example = "editor-user")
    String createdBy,
    @Schema(description = "Letzte Aktualisierung in UTC.", example = "2026-02-15T15:01:00Z")
    Instant updatedAt,
    @Schema(description = "Zuletzt geändert von.", example = "editor-user")
    String updatedBy
) {
}
