Profile: BrokerAuditEvent
Parent: AuditEvent
Id: broker-audit-event
Title: "Query Broker AuditEvent"
Description: """
Profil für AuditEvent-Ressourcen, die Verarbeitungsschritte dokumentieren.
Definiert standardisierte `entity.detail`-Keys für maschinenlesbare Protokolle.
"""

* ^url = "https://querybroker.example.org/fhir/StructureDefinition/BrokerAuditEvent"
* ^version = "0.1.0"
* ^status = #active

* type 1..1
* action 1..1
* period 0..1 MS
* recorded 1..1
* outcome 1..1

* agent 1..*
* agent.type 0..1 MS
* agent.who 1..1
* agent.who only Reference(Device or Organization)
* agent.requestor 1..1

* source 1..1
* source.observer 1..1
* source.observer only Reference(Device or Organization)

* entity 0..* MS
* entity.what 0..1 MS
* entity.detail 0..* MS
* entity.detail ^slicing.discriminator.type = #value
* entity.detail ^slicing.discriminator.path = "type"
* entity.detail ^slicing.rules = #open

* entity.detail contains
    operation 0..1 and
    pseudonymDomain 0..1 and
    sourceSystem 0..1 and
    profileValidation 0..1 and
    resultCount 0..1 and
    durationMs 0..1

* entity.detail[operation].type = "operation"
* entity.detail[operation] ^short = "Name der Operation (z.B. GetConditions)"
* entity.detail[pseudonymDomain].type = "pseudonym-domain"
* entity.detail[pseudonymDomain] ^short = "URI der Pseudonymisierungsdomäne"
* entity.detail[sourceSystem].type = "source-system"
* entity.detail[sourceSystem] ^short = "Bezeichnung des Quellsystems"
* entity.detail[profileValidation].type = "profile-validation"
* entity.detail[profileValidation] ^short = "Ergebnis: passed | failed | skipped"
* entity.detail[resultCount].type = "result-count"
* entity.detail[resultCount] ^short = "Anzahl der fachlichen Ressourcen"
* entity.detail[durationMs].type = "duration-ms"
* entity.detail[durationMs] ^short = "Verarbeitungsdauer in Millisekunden"
