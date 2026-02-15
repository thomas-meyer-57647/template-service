package de.innologic.templateservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

@Schema(name = "RenderRequest", description = "Request für Render- oder Preview-Ausführung.")
public record RenderRequest(
    @NotNull
    @Schema(description = "Template-Familien-ID.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
    UUID templateId,

    @Schema(description = "Optionale Versionsnummer; bei Render wird ignoriert und APPROVED genutzt.", example = "2")
    Integer versionNo,

    @Schema(
        description = "Key-Value-Modell für Placeholder-Ersatz.",
        example = "{\"customerName\":\"Max Mustermann\",\"invoiceNo\":\"INV-2026-1001\"}"
    )
    Map<String, Object> model
) {
}
