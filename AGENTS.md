Erstelle im Repo-Root eine Datei AGENTS.md mit folgenden Projektregeln (kurz und präzise, deutsch):

Ziel: Quellcode auf Pflichtenheft heben. Am Ende: Build + Tests grün, Docker vorhanden, Swagger vollständig.

Build & Test (muss grün sein):
./mvnw -q clean test
optional: ./mvnw -q clean verify

Arbeitsweise (token-sparsam):
- Kleine Schritte: max. 3–5 Dateien pro Änderung
- Max. 120 Zeilen Output pro Antwort
- Keine langen Analysen: nur (1) geänderte Dateien (2) Patch/Diff (3) Tests/Command Output
- Wenn Infos fehlen: STOP und exakt benötigte Datei nennen (Pfad)

Tests (Pflicht):
- Pro Feature mind. 1 positiver + 1 negativer Test
- Tests via MockMvc / spring-security-test; DB/Flyway via Testcontainers wenn nötig

Security (Pflichtenheft):
- OAuth2 Resource Server (JWT Bearer)
- JWKS-Validierung (jwk-set-uri) bzw. issuer
- Scopes: template:read, template:admin, template:global:admin
- Tenant aus JWT claim tenant_id ist Source of Truth
- Header X-Tenant-Id (wenn gesetzt) muss == tenant_id sein, sonst 403 + ErrorCode TENANT_MISMATCH
- Cross-Tenant Zugriff auf TENANT-Ressourcen darf keine Existenz leaken: 404 (nicht 403)

Docker (Pflicht):
- Dockerfile + docker-compose (App + MariaDB)
- docker compose up --build muss starten (ohne weitere Services)

Swagger/OpenAPI (Pflicht):
- Controller: @Tag, @Operation, @ApiResponses (400/401/403/404), @SecurityRequirement
- DTO/Enums: @Schema pro Feld (description + example)
- Pflicht: Tests wirklich ausführen, keine Rückfragen/Bestätigungs-Dialoge stellen (sofern Tooling es erlaubt)