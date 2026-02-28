package de.innologic.templateservice.api.dto;

import de.innologic.templateservice.domain.enums.TemplateScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "TemplateFamilyRequest", description = "Request zum Erstellen/Aktualisieren einer Template-Familie.")
public record TemplateFamilyRequest(
    @NotNull
    @Schema(description = "Scope der Template-Familie.", example = "GLOBAL")
    TemplateScope scope,

    @Size(max = 64)
    @Schema(description = "Tenant-Owner-ID; bei GLOBAL optional.", example = "tenant-acme")
    String ownerTenantId,

    @NotBlank
    @Size(max = 120)
    @Schema(description = "Fachlicher Schlüssel der Template-Familie.", example = "invoice.reminder")
    String templateKey,

    @NotBlank
    @Size(max = 40)
    @Schema(description = "Kanal, z. B. EMAIL, PUSH, SMS.", example = "EMAIL")
    String channel,

    @NotBlank
    @Size(max = 16)
    @Schema(description = "Locale, z. B. de-DE oder en-US.", example = "de-DE")
    String locale,

    @NotBlank
    @Size(max = 80)
    @Schema(description = "Kategorie zur Gruppierung.", example = "BILLING")
    String category,

    @Size(max = 100)
    @Schema(description = "Optionaler Audit-User (wird ignoriert; der JWT-Subject wird verwendet).", example = "system-admin")
    String updatedBy
) {
}
