Der Query Broker ist eine föderierte Integrationsarchitektur, die Datenanfragen an mehrere Primärdatenquellen (PDS) verteilt, deren Antworten aggregiert und profilkonforme FHIR R4 Bundles zurückliefert.

Dieser ImplementationGuide definiert die FHIR-Infrastrukturprofile des Broker-Protokolls sowie eine exemplarische OperationDefinition mit zugehörigen Nachrichtenverträgen.

### Profile

| Profil                                                                        | Basis-Ressource | Zweck                                                                                                   |
| ----------------------------------------------------------------------------- | --------------- | ------------------------------------------------------------------------------------------------------- |
| [BrokerMessageHeader](StructureDefinition-broker-message-header.html)         | MessageHeader   | Nachrichtenformat mit Routing (`destination`), Operationsreferenz (`eventUri`) und Response-Korrelation |
| [BrokerRequestParameters](StructureDefinition-broker-request-parameters.html) | Parameters      | Pseudonym-Slicing und offene operationsspezifische Parameter                                            |
| [BrokerProvenance](StructureDefinition-broker-provenance.html)                | Provenance      | Datenherkunft: Standort, Connector, Quellsystem                                                         |
| [BrokerAuditEvent](StructureDefinition-broker-audit-event.html)               | AuditEvent      | Verarbeitungsprotokoll mit standardisierten Detail-Keys                                                 |

### Exemplarische Operation

| Artefakt                                                                                        | Zweck                                                    |
| ----------------------------------------------------------------------------------------------- | -------------------------------------------------------- |
| [GetConditions (OperationDefinition)](OperationDefinition-GetConditions.html)                   | Operationssemantik: Parameter, Typen, Kardinalitäten     |
| [GetConditionsRequest (MessageDefinition)](MessageDefinition-GetConditionsRequest.html)         | Nachrichtenvertrag: Pflicht-Payloads, erlaubte Antworten |
| [GetConditionsResponse (MessageDefinition)](MessageDefinition-GetConditionsResponse.html)       | Antwortvertrag                                           |
| [GetConditionsResponseGraph (GraphDefinition)](GraphDefinition-GetConditionsResponseGraph.html) | Payload-Struktur                                         |

### Weiterführende Dokumentation

- [Architekturdokumentation (Arc42)](https://github.com/[org]/pds-query-broker/blob/main/docs/ARCHITECTURE.md)
- [PDS-Integrationsleitfaden](https://github.com/[org]/pds-query-broker/blob/main/INTEGRATION.md)
- [Contributing](https://github.com/[org]/pds-query-broker/blob/main/CONTRIBUTING.md)
