package de.innologic.templateservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ValidateTemplateRequest", description = "Request zur statischen Template-Validierung.")
public record ValidateTemplateRequest(
    @Schema(description = "Optionales Subject-Template.", example = "Hallo {{customerName}}")
    String subjectTpl,

    @NotBlank
    @Schema(description = "Body-Template mit Placeholdern im Format {{name}}.", example = "Rechnung {{invoiceNo}} für {{customerName}}")
    String bodyTpl,

    @Size(max = 10000)
    @Schema(description = "Deklarierte Placeholder als JSON-Array oder String-Liste.", example = "[\"customerName\",\"invoiceNo\"]")
    String placeholders
) {
}
