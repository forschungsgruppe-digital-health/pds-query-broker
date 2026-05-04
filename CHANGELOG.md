# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-05-01

_Initial release — Konzept- und Architekturdokumentation._

### Added

- README.md mit Architekturüberblick, Komponentendiagramm (Mermaid), Basisoperationen-Tabelle, Sequenzdiagramm und Standards-Referenzen
- ARCHITECTURE.md als Arc42-Architekturdokumentation (v9.0) mit allen 12 Abschnitten: Einführung und Ziele, Randbedingungen, Kontextabgrenzung (fachlich + technisch), Lösungsstrategie, Bausteinsicht (3 Ebenen mit Komponentenbeschreibungstabellen), Laufzeitsicht (Sequenzdiagramm `$GetConditions`), Verteilungssicht, Querschnittliche Konzepte (Mindmap + 7 Unterabschnitte), Architekturentscheidungen (8 ADRs), Qualitätsanforderungen (Überblick + 6 Szenarien nach arc42 v9.0 / ISO 25010:2023), Risiken und technische Schulden, Glossar
- CONTRIBUTING.md mit Entwicklungs-Setup, PDS-Connector-Implementierung (Java-Beispiel), Konformitätstests, Anleitung zur Definition neuer Operationen (Tripel OperationDefinition + MessageDefinition + GraphDefinition), Broker/SDK-Kernklassen, Code-Konventionen und Release-Prozess
- INTEGRATION.md als sprachagnostischer Implementierungsleitfaden für PDS-Entwickler mit 11 Abschnitten: Stub-Generierung (AsyncAPI CLI), Konfiguration (YAML), OperationDefinition-Interpretation, Handler-Vertrag (Pseudocode), Teilaufgaben-Flowchart, Parameter-Extraktion, FHIR-Mapping-Checkliste, Handler-Registrierung, RabbitMQ-Queue-Setup, Startup-Flow (Sequenzdiagramm), Konformitätstests, Vorgehen bei neuen Operationen
- CHANGELOG.md nach Keep a Changelog 1.1.0

### Architekturentscheidungen in dieser Version

- FHIR R4 Messaging als Nachrichtenformat (Bundle type `message`) statt proprietärem JSON-Envelope (ADR-002)
- Tripel OperationDefinition + MessageDefinition + GraphDefinition als Nachrichtenvertrag mit MII-KDS-Profilbindung über `targetProfile`, `focus.profile`, `target.profile` (ADR-003)
- CapabilityStatement.messaging als Capability-Discovery-Mechanismus statt proprietärem `tools/list` (ADR-004)
- Pseudonyme als FHIR Identifier in der Parameters-Ressource, nicht als MessageHeader-Extension (ADR-005)
- Adapter-Pattern für PDS-Connectoren (ADR-001)
- FHIR Provenance + AuditEvent für Daten-Provenienz und Verarbeitungsprotokoll (ADR-008)
- OperationDefinition-Namen in PascalCase (`GetConditions`, `GetLabResults` etc.) gemäß FHIR Constraint opd-0 (ADR-007)
- AsyncAPI Base-Spec nur für AMQP-Topologie, Operationssemantik vollständig in FHIR-Ressourcen
- FHIR IG-Lifecycle für Katalog-Erstellung und -Bereitstellung (FSH → SUSHI → IG Publisher → FHIR-Paket → Katalog-Server)
- Fanout Exchange als Einstiegsarchitektur, Topic Exchange mit `pds.{pdsId}.*` als Zielarchitektur (ADR-006)

[Unreleased]: https://github.com/[org]/pds-query-broker/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/[org]/pds-query-broker/releases/tag/v0.1.0
