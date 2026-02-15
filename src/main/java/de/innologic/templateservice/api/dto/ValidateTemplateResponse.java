package de.innologic.templateservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ValidateTemplateResponse", description = "Ergebnis der Template-Validierung.")
public record ValidateTemplateResponse(
    @Schema(description = "Gibt an, ob das Template formal gültig ist.", example = "true")
    boolean valid,
    @Schema(description = "Extrahierte Placeholder aus Subject/Body.", example = "[\"customerName\",\"invoiceNo\"]")
    List<String> detectedPlaceholders,
    @Schema(description = "Deklarierte Placeholder (aus Request geparst).", example = "[\"customerName\",\"invoiceNo\"]")
    List<String> declaredPlaceholders,
    @Schema(description = "Fehlerliste; leer bei valid=true.", example = "[]")
    List<String> errors
) {
}
