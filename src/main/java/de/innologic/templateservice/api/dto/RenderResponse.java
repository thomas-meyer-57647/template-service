package de.innologic.templateservice.api.dto;

import de.innologic.templateservice.domain.enums.RenderTarget;
import de.innologic.templateservice.domain.enums.TemplateScope;
import de.innologic.templateservice.domain.enums.TemplateStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "RenderResponse", description = "Ergebnis eines Render-/Preview-Aufrufs.")
public record RenderResponse(
    @Schema(description = "Aufgelöster Scope.", example = "TENANT")
    TemplateScope resolvedScope,
    @Schema(description = "Template-Familien-ID.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
    UUID templateId,
    @Schema(description = "Aufgelöste Locale nach Fallback.", example = "de-DE")
    String resolvedLocale,
    @Schema(description = "Verwendete Versionsnummer.", example = "3")
    Integer versionNo,
    @Schema(description = "Status der gerenderten Version.", example = "APPROVED")
    TemplateStatus status,
    @Schema(description = "Render-Target.", example = "HTML")
    RenderTarget renderTarget,
    @Schema(description = "Content-Type der Ausgabe.", example = "text/html")
    String contentType,
    @Schema(description = "Gerendertes Subject (optional).", example = "Zahlungserinnerung INV-2026-1001")
    String renderedSubject,
    @Schema(description = "Gerenderter Body.", example = "<p>Hallo Max Mustermann, Rechnung INV-2026-1001 ist fällig.</p>")
    String renderedBody,
    @Schema(description = "Warnungen aus dem Rendering-Prozess.", example = "[\"MISSING_KEYS missingKeys=[invoiceNo]\"]")
    List<String> warnings
) {
}
