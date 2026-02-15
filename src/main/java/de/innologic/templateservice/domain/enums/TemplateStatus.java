package de.innologic.templateservice.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Lebenszyklusstatus einer Template-Version.",
    example = "DRAFT"
)
public enum TemplateStatus {
    @Schema(description = "Entwurf, noch nicht freigegeben.")
    DRAFT,
    @Schema(description = "Freigegebene Version für produktives Rendering.")
    APPROVED,
    @Schema(description = "Veraltet, sollte nicht mehr neu verwendet werden.")
    DEPRECATED,
    @Schema(description = "Archiviert und inaktiv.")
    ARCHIVED
}
