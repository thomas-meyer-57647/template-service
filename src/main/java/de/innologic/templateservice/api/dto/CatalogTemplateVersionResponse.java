package de.innologic.templateservice.api.dto;

import de.innologic.templateservice.domain.enums.RenderTarget;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(name = "CatalogTemplateVersionResponse", description = "Version-Metadaten für den Portal-Katalog (ohne Template-Inhalte).")
public record CatalogTemplateVersionResponse(
        @Schema(description = "Version-ID.", example = "01J...VER")
        UUID versionId,
        @Schema(description = "Template-ID.", example = "01J...TPL")
        UUID templateId,
        @Schema(description = "Versionsnummer.", example = "3")
        Integer versionNo,
        @Schema(description = "Status (für Endpoint immer APPROVED).", example = "APPROVED")
        TemplateStatus status,
        @Schema(description = "Render-Target.", example = "HTML")
        RenderTarget renderTarget,
        @Schema(description = "Erstellt am (UTC).", example = "2026-02-17T10:00:00Z")
        Instant createdAt
) {
}

