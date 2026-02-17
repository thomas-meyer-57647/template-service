package de.innologic.templateservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PageResponse", description = "Generische Paging-Antwort.")
public record PageResponse<T>(
        @Schema(description = "Ergebnisliste der aktuellen Seite.")
        List<T> items,
        @Schema(description = "Seitennummer (0-basiert).", example = "0")
        int page,
        @Schema(description = "Seitengröße.", example = "50")
        int size,
        @Schema(description = "Gesamtanzahl aller Treffer.", example = "123")
        long total
) {
}

