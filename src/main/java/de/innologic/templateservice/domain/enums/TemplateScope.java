package de.innologic.templateservice.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Geltungsbereich einer Template-Familie.",
    example = "GLOBAL"
)
public enum TemplateScope {
    @Schema(description = "Globales Template, tenant-unabhängig.")
    GLOBAL,
    @Schema(description = "Tenant-spezifisches Template.")
    TENANT
}
