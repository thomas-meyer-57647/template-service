package de.innologic.templateservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "CatalogTemplateFamilyResponse", description = "Metadaten einer Template-Family für den Portal-Katalog.")
public record CatalogTemplateFamilyResponse(
        @Schema(description = "Template-ID.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
        UUID templateId,
        @Schema(description = "Business-Key.", example = "email.confirmation")
        String templateKey,
        @Schema(description = "Kanal.", example = "EMAIL")
        String channel,
        @Schema(description = "Locale.", example = "de-DE")
        String locale,
        @Schema(description = "Kategorie.", example = "TRANSACTIONAL")
        String category,
        @Schema(description = "Aktive APPROVED Version.", example = "3")
        Integer activeApprovedVersion
) {
}

