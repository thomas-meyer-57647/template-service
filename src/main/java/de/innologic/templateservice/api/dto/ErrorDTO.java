package de.innologic.templateservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(
    name = "ErrorDTO",
    description = "Standardisiertes Fehlerobjekt für API-Fehlerantworten."
)
public record ErrorDTO(
    @Schema(description = "Zeitpunkt des Fehlers in UTC.", example = "2026-02-15T14:58:30Z")
    Instant timestamp,
    @Schema(description = "HTTP-Statuscode.", example = "404")
    int status,
    @Schema(description = "Kurzer Fehlercode bzw. -typ.", example = "Not Found")
    String error,
    @Schema(description = "Fehlerbeschreibung für den Client.", example = "Template family not found: 5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
    String message,
    @Schema(description = "API-Pfad der Anfrage.", example = "/api/v1/template/families/5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
    String path
) {
}
