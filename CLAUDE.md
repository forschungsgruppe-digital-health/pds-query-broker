# CLAUDE.md

Projekthinweise für Claude Code und Cowork-Sitzungen.

## Projektstruktur

```
pds-query-broker/
├── docs/ARCHITECTURE.md     ← Arc42 v9.0 Architekturdokumentation
├── CONTRIBUTING.md           ← Broker/SDK-Entwicklung, neue Operationen
├── INTEGRATION.md        ← Sprachagnostischer PDS-Connector-Leitfaden
├── CHANGELOG.md              ← Keep a Changelog 1.1.0
├── ig/                       ← FHIR ImplementationGuide Projekt
│   ├── sushi-config.yaml     ← IG-Konfiguration
│   ├── ig.ini                ← HL7 Publisher Konfiguration
│   └── input/fsh/            ← FSH-Quellen (Profile, Beispiele)
├── specs/                    ← AsyncAPI-Spezifikationen
├── docker/                   ← Docker Compose Setup
└── .claude/                  ← Sub-Agenten-Konfigurationen
```

## Konventionen

- **Sprache**: Dokumentation auf Deutsch, Code/Config auf Englisch
- **OperationDefinition-Namen**: PascalCase, FHIR Constraint opd-0
- **Präfix**: PDS (Primary Data Source), nicht DIZ
- **Profilbindung**: Optional, projektspezifisch — MII KDS ist ein Beispiel, nicht Vorgabe
- **AMQP-Topologie**: `pds.broadcast`, `req.{pdsId}`, `responses.{systemId}`, `pds.dlq`
- **Response-Routing**: `MessageHeader.destination.endpoint` + AMQP `replyTo`
- **Mermaid-Diagramme**: Keine Farben/Styles, nur Default-Rendering
- **Code-Blöcke**: Nur für echten Code/Pseudocode/Shell. Strukturierte Inhalte als Tabellen/Listen/Mermaid.

## Häufige Aufgaben

### FHIR IG kompilieren

```bash
cd ig && sushi build
```

Erwartet: 0 Errors, 0 Warnings.

### IG Publisher ausführen (erzeugt HTML)

```bash
cd ig && ./_updatePublisher.sh && ./_genonce.sh
```

### Docker-Umgebung starten

```bash
cd docker && cp .env.example .env && docker compose up -d
```

### AsyncAPI validieren

```bash
asyncapi validate specs/pds-connector-base.yaml
```

## Architektur-Kernkonzepte

1. **FHIR Messaging** — Alle Nachrichten sind FHIR R4 Bundles (type: message)
2. **Tripel** — Jede Operation besteht aus OperationDefinition + MessageDefinition + GraphDefinition
3. **Profilbindung** — Optional via `targetProfile`, `focus.profile`, `target.profile`
4. **Adapter-Pattern** — Connectoren übersetzen zwischen Broker-Protokoll und lokalem System
5. **Broadcast + Self-Filtering** — Fanout Exchange, Connector filtert nach Pseudonym-Domäne
6. **Multi-Client-Routing** — `MessageHeader.destination` + systemspezifische Response-Queues
7. **Provenance + AuditEvent** — Datenherkunft und Verarbeitungsprotokoll als FHIR-Ressourcen
