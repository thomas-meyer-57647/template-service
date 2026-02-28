package de.innologic.templateservice.api.dto;

import de.innologic.templateservice.domain.enums.RenderTarget;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(name = "TemplateVersionRequest", description = "Request zum Erstellen/Aktualisieren einer Template-Version.")
public record TemplateVersionRequest(
    @Schema(description = "Versionsnummer; bei Create optional, wird sonst automatisch vergeben.", example = "2")
    Integer versionNo,

    @Schema(description = "Status der Version (serverseitig immer DRAFT).", example = "DRAFT")
    TemplateStatus status,

    @NotNull
    @Schema(description = "Render-Target der Ausgabe.", example = "HTML")
    RenderTarget renderTarget,

    @Schema(description = "Optionales Subject-Template, z. B. für E-Mails.", example = "Zahlungserinnerung {{invoiceNo}}")
    String subjectTpl,

    @NotBlank
    @Schema(description = "Template-Body mit Placeholdern im Format {{name}}.", example = "<p>Hallo {{customerName}}, Rechnung {{invoiceNo}} ist fällig.</p>")
    String bodyTpl,

    @Size(max = 10000)
    @Schema(description = "Placeholder als JSON-Array oder String-Liste.", example = "[\"customerName\",\"invoiceNo\"]")
    String placeholders,

    @Size(max = 100)
    @Schema(description = "Optionaler Audit-User (wird ignoriert; der JWT-Subject wird verwendet).", example = "editor-user")
    String updatedBy
) {
}
