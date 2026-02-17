package de.innologic.templateservice.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Verhalten bei fehlenden Placeholder-Werten.")
public enum MissingKeyPolicy {
    @Schema(description = "Fehlende Keys brechen das Rendering mit Fehler ab.")
    FAIL,
    @Schema(description = "Fehlende Tokens bleiben unverändert im Output.")
    KEEP_TOKEN,
    @Schema(description = "Fehlende Tokens werden mit leerem String ersetzt.")
    EMPTY
}

