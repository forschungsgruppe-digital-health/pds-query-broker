The Query Broker is a federated integration architecture that distributes data requests to multiple primary data sources (PDS), aggregates their responses, and returns profile-conformant FHIR R4 Bundles.

This ImplementationGuide defines the FHIR infrastructure profiles of the broker protocol as well as an exemplary OperationDefinition with its associated message contracts.

### Profiles

| Profile | Base resource | Purpose |
|--------|----------------|-------|
| [BrokerMessageHeader](StructureDefinition-broker-message-header.html) | MessageHeader | Message format with routing (`destination`), operation reference (`eventUri`), and response correlation |
| [BrokerRequestParameters](StructureDefinition-broker-request-parameters.html) | Parameters | Pseudonym slicing and open operation-specific parameters |
| [BrokerProvenance](StructureDefinition-broker-provenance.html) | Provenance | Data provenance: site, connector, source system |
| [BrokerAuditEvent](StructureDefinition-broker-audit-event.html) | AuditEvent | Processing log with standardized detail keys |

### Exemplary operation

| Artifact | Purpose |
|----------|-------|
| [GetConditions (OperationDefinition)](OperationDefinition-GetConditions.html) | Operation semantics: parameters, types, cardinalities |
| [GetConditionsRequest (MessageDefinition)](MessageDefinition-GetConditionsRequest.html) | Message contract: mandatory payloads, allowed responses |
| [GetConditionsResponse (MessageDefinition)](MessageDefinition-GetConditionsResponse.html) | Response contract |
| [GetConditionsResponseGraph (GraphDefinition)](GraphDefinition-GetConditionsResponseGraph.html) | Payload structure |

### Further documentation

- [Architecture documentation (Arc42)](https://github.com/[org]/query-broker/blob/main/docs/ARCHITECTURE.md)
- [PDS integration guide](https://github.com/[org]/query-broker/blob/main/PDS_INTEGRATION.md)
- [Contributing](https://github.com/[org]/query-broker/blob/main/CONTRIBUTING.md)
