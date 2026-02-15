# Test-Suite (JUnit 5)

Diese Test-Suite deckt folgende Bereiche ab:

- API/Controller-Integration mit `MockMvc`
- Security-Tests mit Mock-JWT (`tenant_id`, Rollen `platform_admin`, `tenant_admin`, `user`)
- DB-Integration mit Testcontainers (MariaDB) und Flyway-Migrationen
- Rendering-Tests für Placeholder und MissingKeyPolicy (`FAIL`, `KEEP_TOKEN`, `EMPTY`) inkl. HTML-Escaping
- Governance-Tests für `GLOBAL` vs `TENANT`, No-Shadowing und reserved keys

## Ausführen

```bash
mvn test
```

## Hinweise zu Testcontainers

- Die DB-Integrationstests starten eine temporäre MariaDB per Docker.
- Flyway-Migrationen unter `src/main/resources/db/migration` werden beim Teststart angewendet.
- Voraussetzung: Docker Engine läuft lokal.
