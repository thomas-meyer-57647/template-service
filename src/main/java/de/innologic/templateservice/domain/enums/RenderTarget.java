package de.innologic.templateservice.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Ausgabetyp des gerenderten Templates.",
    example = "HTML"
)
public enum RenderTarget {
    @Schema(description = "Klartextausgabe.")
    TEXT,
    @Schema(description = "HTML-Ausgabe.")
    HTML
}
