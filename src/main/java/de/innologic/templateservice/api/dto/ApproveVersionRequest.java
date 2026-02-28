package de.innologic.templateservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(name = "ApproveVersionRequest", description = "Request zum Freigeben einer bestimmten Template-Version.")
public record ApproveVersionRequest(
    @Size(max = 100)
    @Schema(description = "Optionaler Audit-User (wird ignoriert; JWT-Subject wird verwendet).", example = "release-manager")
    String updatedBy
) {
}
