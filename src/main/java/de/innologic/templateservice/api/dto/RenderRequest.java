package de.innologic.templateservice.api.dto;

import de.innologic.templateservice.domain.enums.MissingKeyPolicy;
import de.innologic.templateservice.domain.enums.TemplateScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

import java.util.Map;
import java.util.UUID;

@Schema(name = "RenderRequest", description = "Request für Render- oder Preview-Ausführung.")
public record RenderRequest(
    @Deprecated
    @Schema(
        description = "Template-Familien-ID (deprecated: Business-Key bevorzugt).",
        example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f",
        deprecated = true
    )
    UUID templateId,

    @Schema(description = "Scope für Business-Key Resolve.", example = "TENANT")
    TemplateScope scope,

    @Size(max = 120)
    @Schema(description = "Template-Key für Resolve.", example = "email.confirmation")
    String templateKey,

    @Size(max = 40)
    @Schema(description = "Kanal für Resolve.", example = "EMAIL")
    String channel,

    @NotBlank
    @Schema(description = "Gewünschte Locale im Request (wird für Fallback genutzt).", example = "de-DE")
    String locale,

    @Schema(description = "Optionale Versionsnummer; bei Render wird ignoriert und APPROVED genutzt.", example = "2")
    Integer versionNo,

    @Schema(description = "Missing-Key-Verhalten.", example = "FAIL")
    MissingKeyPolicy missingKeyPolicy,

    @Schema(
        description = "Key-Value-Variablen für Placeholder-Ersatz.",
        example = "{\"customerName\":\"Max Mustermann\",\"invoiceNo\":\"INV-2026-1001\"}"
    )
    Map<String, Object> variables,

    @Deprecated
    @Schema(
        description = "Alias für variables (deprecated).",
        example = "{\"customerName\":\"Max Mustermann\"}",
        deprecated = true
    )
    Map<String, Object> model
) {
    public Map<String, Object> effectiveVariables() {
        if (variables != null) {
            return variables;
        }
        return model;
    }

    public MissingKeyPolicy effectiveMissingKeyPolicy() {
        return missingKeyPolicy == null ? MissingKeyPolicy.FAIL : missingKeyPolicy;
    }

    @AssertTrue(message = "Either templateId or (scope + templateKey + channel) must be provided")
    public boolean hasResolvableIdentity() {
        if (templateId != null) {
            return true;
        }
        return scope != null && notBlank(templateKey) && notBlank(channel);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
