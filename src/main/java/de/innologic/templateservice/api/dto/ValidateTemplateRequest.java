package de.innologic.templateservice.api.dto;

import de.innologic.templateservice.domain.enums.MissingKeyPolicy;
import de.innologic.templateservice.domain.enums.TemplateScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Schema(name = "ValidateTemplateRequest", description = "Request zur statischen Template-Validierung.")
public record ValidateTemplateRequest(
    @Schema(description = "Optional: Scope für Resolve-Kontext.", example = "TENANT")
    TemplateScope scope,

    @Schema(description = "Optional: Template-Key für Resolve-Kontext.", example = "email.confirmation")
    String templateKey,

    @Schema(description = "Optional: Kanal für Resolve-Kontext.", example = "EMAIL")
    String channel,

    @Schema(description = "Optional: Locale für Resolve/Fallback-Kontext.", example = "de-DE")
    String locale,

    @Schema(description = "Optionales Subject-Template.", example = "Hallo {{customerName}}")
    String subjectTpl,

    @NotBlank
    @Schema(description = "Body-Template mit Placeholdern im Format {{name}}.", example = "Rechnung {{invoiceNo}} für {{customerName}}")
    String bodyTpl,

    @Size(max = 10000)
    @Schema(description = "Deklarierte Placeholder als JSON-Array oder String-Liste.", example = "[\"customerName\",\"invoiceNo\"]")
    String placeholders,

    @Schema(description = "Optionales Missing-Key-Verhalten für Validierungs-Simulation.", example = "FAIL")
    MissingKeyPolicy missingKeyPolicy,

    @Schema(
        description = "Optionale Variablen für Validierungs-Simulation.",
        example = "{\"customerName\":\"Max\"}"
    )
    Map<String, Object> variables
) {
}
