package de.innologic.templateservice.api.controller;

import de.innologic.templateservice.api.dto.ApproveVersionRequest;
import de.innologic.templateservice.api.dto.CatalogTemplateFamilyResponse;
import de.innologic.templateservice.api.dto.CatalogTemplateVersionResponse;
import de.innologic.templateservice.api.dto.ErrorDTO;
import de.innologic.templateservice.api.dto.PageResponse;
import de.innologic.templateservice.api.dto.RenderRequest;
import de.innologic.templateservice.api.dto.RenderResponse;
import de.innologic.templateservice.api.dto.TemplateFamilyRequest;
import de.innologic.templateservice.api.dto.TemplateFamilyResponse;
import de.innologic.templateservice.api.dto.TemplateVersionRequest;
import de.innologic.templateservice.api.dto.TemplateVersionResponse;
import de.innologic.templateservice.api.dto.ValidateTemplateRequest;
import de.innologic.templateservice.api.dto.ValidateTemplateResponse;
import de.innologic.templateservice.domain.enums.TemplateScope;
import de.innologic.templateservice.security.TenantContext;
import de.innologic.templateservice.security.TenantContextResolver;
import de.innologic.templateservice.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/template")
@Validated
@Tag(
    name = "Template API",
    description = "CRUD, Freigabe und Rendering für Template-Familien und Template-Versionen."
)
@SecurityRequirement(name = "bearerAuth")
public class TemplateController {

    private static final String GLOBAL_OWNER = "__GLOBAL__";

    private final TemplateService templateService;
    private final TenantContextResolver tenantContextResolver;

    public TemplateController(
            TemplateService templateService,
            TenantContextResolver tenantContextResolver
    ) {
        this.templateService = templateService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/render")
    @Operation(
        summary = "Template rendern (nur APPROVED)",
        description = "Rendert ausschließlich eine APPROVED Version der angegebenen Template-Familie."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Render erfolgreich",
            content = @Content(schema = @Schema(implementation = RenderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Template-Familie nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "422", description = "Keine APPROVED Version oder fehlende Placeholder-Werte",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "500", description = "Technischer Fehler",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public RenderResponse render(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Render-Request mit Modeldaten.",
            content = @Content(examples = @ExampleObject(
                value = """
                    {
                      "scope": "TENANT",
                      "templateKey": "email.confirmation",
                      "channel": "EMAIL",
                      "locale": "de-DE",
                      "missingKeyPolicy": "FAIL",
                      "variables": {
                        "firstName": "Max",
                        "confirmLink": "https://example.test/confirm"
                      },
                      "templateId": "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f"
                    }
                    """
            ))
        )
        @Valid @RequestBody RenderRequest request
    ) {
        return templateService.renderApproved(request);
    }

    @PostMapping("/preview")
    @Operation(
        summary = "Template-Preview rendern (DRAFT erlaubt)",
        description = "Rendert eine konkrete Version oder, falls nicht angegeben, die neueste Version unabhängig vom Status."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preview erfolgreich",
            content = @Content(schema = @Schema(implementation = RenderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Template oder Version nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "422", description = "Fehlende Placeholder-Werte",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public RenderResponse preview(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Preview-Request.",
            content = @Content(examples = @ExampleObject(
                value = """
                    {
                      "scope": "TENANT",
                      "templateKey": "email.confirmation",
                      "channel": "EMAIL",
                      "locale": "en-GB",
                      "versionNo": 2,
                      "missingKeyPolicy": "FAIL",
                      "variables": {
                        "firstName": "Max",
                        "confirmLink": "https://example.test/confirm"
                      }
                    }
                    """
            ))
        )
        @Valid @RequestBody RenderRequest request
    ) {
        return templateService.preview(request);
    }

    @PostMapping("/validate")
    @Operation(
        summary = "Template validieren",
        description = "Prüft Placeholder-Syntax und Konsistenz zwischen deklarierten und tatsächlich verwendeten Placeholdern."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Validierung durchgeführt",
            content = @Content(schema = @Schema(implementation = ValidateTemplateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public ValidateTemplateResponse validate(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Template-Validierungsrequest.",
            content = @Content(examples = @ExampleObject(
                value = """
                    {
                      "scope": "TENANT",
                      "templateKey": "email.confirmation",
                      "channel": "EMAIL",
                      "locale": "de-DE",
                      "subjectTpl": "Hallo {{customerName}}",
                      "bodyTpl": "Rechnung {{invoiceNo}} für {{customerName}}",
                      "placeholders": "[\\"customerName\\",\\"invoiceNo\\"]"
                    }
                    """
            ))
        )
        @Valid @RequestBody ValidateTemplateRequest request
    ) {
        return templateService.validate(request);
    }

    @GetMapping("/catalog")
    @Operation(
        summary = "Catalog-Liste für Portal-UI",
        description = "Liefert Template-Metadaten für den Katalog. Pinning erfolgt im Portal, nicht im template-service."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Erfolgreich",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public PageResponse<CatalogTemplateFamilyResponse> catalog(
            @Parameter(description = "Scope-Filter.", example = "GLOBAL")
            @RequestParam(defaultValue = "GLOBAL") TemplateScope scope,
            @Parameter(description = "Kanal.", example = "EMAIL")
            @RequestParam String channel,
            @Parameter(description = "Locale.", example = "de-DE")
            @RequestParam String locale,
            @Parameter(description = "Seite (0-basiert).", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Seitengröße (max. 200).", example = "50")
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
            @Parameter(description = "Sortierung: field,DESC|ASC", example = "templateKey,ASC")
            @RequestParam(defaultValue = "templateKey,ASC") String sort
    ) {
        return templateService.catalogFamilies(scope, channel, locale, page, size, sort);
    }

    @GetMapping("/catalog/{templateId}/approved-versions")
    @Operation(
        summary = "APPROVED Versionen für Portal-UI",
        description = "Liefert APPROVED Versions-Metadaten für Tenant-Pinning im Portal (Pinning selbst nicht im template-service)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Erfolgreich",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Template nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public PageResponse<CatalogTemplateVersionResponse> catalogApprovedVersions(
            @Parameter(description = "Template-ID.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
            @PathVariable UUID templateId,
            @Parameter(description = "Seite (0-basiert).", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Seitengröße (max. 200).", example = "50")
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
            @Parameter(description = "Sortierung: field,DESC|ASC", example = "versionNo,DESC")
            @RequestParam(defaultValue = "versionNo,DESC") String sort
    ) {
        return templateService.catalogApprovedVersions(templateId, page, size, sort);
    }

    @PostMapping("/families")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("(@scopeAuthorizationHelper.hasScope('template:global:admin') and #request.scope.name() == 'GLOBAL') or (@scopeAuthorizationHelper.hasScope('template:admin') and #request.scope.name() == 'TENANT')")
    @Operation(summary = "Template-Familie anlegen", description = "Erzeugt eine neue Template-Familie.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Erfolgreich angelegt",
            content = @Content(schema = @Schema(implementation = TemplateFamilyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "409", description = "Unique-Constraint verletzt",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public TemplateFamilyResponse createFamily(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(examples = @ExampleObject(
                value = """
                    {
                      "scope": "TENANT",
                      "ownerTenantId": "tenant-acme",
                      "templateKey": "invoice.reminder",
                      "channel": "EMAIL",
                      "locale": "de-DE",
                      "category": "BILLING",
                      "updatedBy": "editor-user"
                    }
                    """
            ))
        )
        @Valid @RequestBody TemplateFamilyRequest request
    ) {
        TenantContext tenantContext = tenantContextResolver.resolveRequired();
        return templateService.createFamily(normalizeFamilyRequest(request, tenantContext));
    }

    @GetMapping("/families")
    @Operation(summary = "Template-Familien auflisten", description = "Liefert Templates paginiert und sortiert.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Erfolgreich",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public PageResponse<TemplateFamilyResponse> listFamilies(
            @Parameter(description = "Seite (0-basiert).", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Seitengröße (max. 200).", example = "50")
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
            @Parameter(description = "Sortierung: field,DESC|ASC", example = "createdAt,DESC")
            @RequestParam(defaultValue = "createdAt,DESC") String sort
    ) {
        return templateService.listFamilies(page, size, sort);
    }

    @GetMapping("/families/{templateId}")
    @Operation(summary = "Template-Familie laden", description = "Lädt eine Template-Familie per ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Erfolgreich",
            content = @Content(schema = @Schema(implementation = TemplateFamilyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public TemplateFamilyResponse getFamily(
        @Parameter(description = "Template-Familien-ID.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
        @PathVariable UUID templateId
    ) {
        return templateService.getFamily(templateId);
    }

    @PutMapping("/families/{templateId}")
    @PreAuthorize("(@scopeAuthorizationHelper.hasScope('template:global:admin') and #request.scope.name() == 'GLOBAL') or (@scopeAuthorizationHelper.hasScope('template:admin') and #request.scope.name() == 'TENANT')")
    @Operation(summary = "Template-Familie aktualisieren", description = "Aktualisiert die Stammdaten einer Template-Familie.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Erfolgreich aktualisiert",
            content = @Content(schema = @Schema(implementation = TemplateFamilyResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "409", description = "Unique-Constraint verletzt",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public TemplateFamilyResponse updateFamily(
        @Parameter(description = "Template-Familien-ID.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
        @PathVariable UUID templateId,
        @Valid @RequestBody TemplateFamilyRequest request
    ) {
        TenantContext tenantContext = tenantContextResolver.resolveRequired();
        return templateService.updateFamily(templateId, normalizeFamilyRequest(request, tenantContext));
    }

    @DeleteMapping("/families/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("(@scopeAuthorizationHelper.hasScope('template:global:admin') and @templateService.isGlobalFamily(#templateId)) or (@scopeAuthorizationHelper.hasScope('template:admin') and !@templateService.isGlobalFamily(#templateId))")
    @Operation(summary = "Template-Familie löschen", description = "Löscht eine Template-Familie inkl. aller Versionen.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Erfolgreich gelöscht"),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public void deleteFamily(
        @Parameter(description = "Template-Familien-ID.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
        @PathVariable UUID templateId
    ) {
        templateService.deleteFamily(templateId);
    }

    @PostMapping("/families/{templateId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("(@scopeAuthorizationHelper.hasScope('template:global:admin') and @templateService.isGlobalFamily(#templateId)) or (@scopeAuthorizationHelper.hasScope('template:admin') and !@templateService.isGlobalFamily(#templateId))")
    @Operation(summary = "Version anlegen", description = "Erzeugt eine neue Template-Version in einer Familie.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Erfolgreich angelegt",
            content = @Content(schema = @Schema(implementation = TemplateVersionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Template-Familie nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "409", description = "Version bereits vorhanden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public TemplateVersionResponse createVersion(
        @Parameter(description = "Template-Familien-ID.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
        @PathVariable UUID templateId,
        @Valid @RequestBody TemplateVersionRequest request
    ) {
        return templateService.createVersion(templateId, request);
    }

    @GetMapping("/families/{templateId}/versions")
    @Operation(summary = "Versionen auflisten", description = "Liefert Versionen paginiert und sortiert.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Erfolgreich",
            content = @Content(schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Template-Familie nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public PageResponse<TemplateVersionResponse> listVersions(
            @Parameter(description = "Template-Familien-ID.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
            @PathVariable UUID templateId,
            @Parameter(description = "Seite (0-basiert).", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Seitengröße (max. 200).", example = "50")
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
            @Parameter(description = "Sortierung: field,DESC|ASC", example = "versionNo,DESC")
            @RequestParam(defaultValue = "versionNo,DESC") String sort
    ) {
        return templateService.listVersions(templateId, page, size, sort);
    }

    @GetMapping("/families/{templateId}/versions/{versionNo}")
    @Operation(summary = "Version laden", description = "Lädt eine konkrete Version einer Template-Familie.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Erfolgreich",
            content = @Content(schema = @Schema(implementation = TemplateVersionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public TemplateVersionResponse getVersion(
        @Parameter(description = "Template-Familien-ID.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
        @PathVariable UUID templateId,
        @Parameter(description = "Versionsnummer.", example = "3")
        @PathVariable Integer versionNo
    ) {
        return templateService.getVersion(templateId, versionNo);
    }

    @PutMapping("/families/{templateId}/versions/{versionNo}")
    @PreAuthorize("(@scopeAuthorizationHelper.hasScope('template:global:admin') and @templateService.isGlobalFamily(#templateId)) or (@scopeAuthorizationHelper.hasScope('template:admin') and !@templateService.isGlobalFamily(#templateId))")
    @Deprecated
    @Operation(
        summary = "Version aktualisieren (deprecated)",
        description = "In-Place-Updates sind nicht erlaubt; Änderungen müssen über POST als neue Version erfolgen.",
        deprecated = true
    )
    @ApiResponses({
        @ApiResponse(responseCode = "409", description = "Version ist immutable",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public TemplateVersionResponse updateVersion(
        @Parameter(description = "Template-Familien-ID.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
        @PathVariable UUID templateId,
        @Parameter(description = "Versionsnummer.", example = "2")
        @PathVariable Integer versionNo,
        @Valid @RequestBody TemplateVersionRequest request
    ) {
        return templateService.updateVersion(templateId, versionNo, request);
    }

    @DeleteMapping("/families/{templateId}/versions/{versionNo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("(@scopeAuthorizationHelper.hasScope('template:global:admin') and @templateService.isGlobalFamily(#templateId)) or (@scopeAuthorizationHelper.hasScope('template:admin') and !@templateService.isGlobalFamily(#templateId))")
    @Operation(summary = "Version löschen", description = "Löscht eine konkrete Template-Version.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Erfolgreich gelöscht"),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public void deleteVersion(
        @Parameter(description = "Template-Familien-ID.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
        @PathVariable UUID templateId,
        @Parameter(description = "Versionsnummer.", example = "1")
        @PathVariable Integer versionNo
    ) {
        templateService.deleteVersion(templateId, versionNo);
    }

    @PostMapping("/families/{templateId}/versions/{versionNo}/approve")
    @PreAuthorize("(@scopeAuthorizationHelper.hasScope('template:global:admin') and @templateService.isGlobalFamily(#templateId)) or (@scopeAuthorizationHelper.hasScope('template:admin') and !@templateService.isGlobalFamily(#templateId))")
    @Operation(
        summary = "Version freigeben (APPROVED)",
        description = "Setzt die gewählte Version auf APPROVED und markiert zuvor APPROVED Versionen als DEPRECATED."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Version freigegeben",
            content = @Content(schema = @Schema(implementation = TemplateVersionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültiger Request",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "401", description = "Nicht authentifiziert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "403", description = "Nicht autorisiert",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class))),
        @ApiResponse(responseCode = "404", description = "Template-Familie oder Version nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorDTO.class)))
    })
    public TemplateVersionResponse approveVersion(
        @Parameter(description = "Template-Familien-ID.", example = "5fbf2f42-7d2f-4fc0-9a76-d0946fc8a28f")
        @PathVariable UUID templateId,
        @Parameter(description = "Versionsnummer, die freigegeben werden soll.", example = "3")
        @PathVariable Integer versionNo,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(examples = @ExampleObject(value = "{\"updatedBy\":\"release-manager\"}"))
        )
        @RequestBody(required = false) ApproveVersionRequest request
    ) {
        return templateService.approveVersion(templateId, versionNo);
    }

    private TemplateFamilyRequest normalizeFamilyRequest(TemplateFamilyRequest request, TenantContext tenantContext) {
        String ownerTenantId = request.scope() == TemplateScope.GLOBAL ? GLOBAL_OWNER : tenantContext.tenantId();
        return new TemplateFamilyRequest(
                request.scope(),
                ownerTenantId,
                request.templateKey(),
                request.channel(),
                request.locale(),
                request.category(),
                tenantContext.actor()
        );
    }
}
