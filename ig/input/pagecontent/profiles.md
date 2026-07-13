This page describes the four infrastructure profiles of the Query Broker protocol.

### BrokerMessageHeader

Profiles `MessageHeader` for FHIR messaging over AMQP. Requires `eventUri` (OperationDefinition URL), `destination` (response routing for multi-client capability), and `source` (AMQP endpoint). In response messages, `response` with a correlation ID and status code is mandatory.

### BrokerRequestParameters

Profiles `Parameters` with open slicing. The slice `pseudonym` (1..*) is a typed `Identifier` with `system` = pseudonymization domain and `value` = pseudonym. Operation-specific parameters are supported through `#open` slicing — they do not have to be declared in the profile but are derived from the respective OperationDefinition.

### BrokerProvenance

Profiles `Provenance` with agent slicing: `performer` (organization/site) and `assembler` (connector software). An entity with role `source` documents the local source system via `identifier` and `display`.

### BrokerAuditEvent

Profiles `AuditEvent` with detail slicing for six standardized keys: `operation`, `pseudonym-domain`, `source-system`, `profile-validation`, `result-count`, `duration-ms`. All keys are optional — which ones are set depends on the processing step.
