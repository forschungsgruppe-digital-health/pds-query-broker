This page describes the infrastructure profiles of the Query Broker protocol: the two message envelopes (request/response Bundle), the four entry-level profiles, and the error model.

### BrokerRequestBundle

Profiles `Bundle` (`type = message`) as the request envelope: first entry a `BrokerMessageHeader` **without** a `response` element (invariant `qb-request-1`), plus exactly one `BrokerRequestParameters`. `timestamp` is mandatory. Additional entries remain open for forward compatibility.

### BrokerResponseBundle

Profiles `Bundle` (`type = message`) as the response envelope — for single connector responses and the aggregated broker response alike: first entry a `BrokerMessageHeader` **with** a `response` element (invariant `qb-response-1`). Result resources travel as open entries (their profile follows from the operation's GraphDefinition/`targetProfile`); errors as `BrokerOperationOutcome`, provenance as `BrokerProvenance`, processing logs as `BrokerAuditEvent`.

### BrokerOperationOutcome

Profiles `OperationOutcome` as the protocol's error model: every issue carries a machine-readable broker error code in `issue.details` (extensible binding to the `BrokerErrorCodes` CodeSystem: `timeout`, `unsupported-operation`, `no-capable-pds`, `pds-error`, `validation-error`) alongside the generic FHIR `issue.code`, plus human-readable `diagnostics`.

### BrokerMessageHeader

Profiles `MessageHeader` for FHIR messaging over AMQP. Requires `eventUri` (OperationDefinition URL), `destination` (response routing for multi-client capability), and `source` (AMQP endpoint). In response messages, `response` with a correlation ID and status code is mandatory.

### BrokerRequestParameters

Profiles `Parameters` with open slicing. The slice `pseudonym` (1..*) is a typed `Identifier` with `system` = pseudonymization domain and `value` = pseudonym. Operation-specific parameters are supported through `#open` slicing — they do not have to be declared in the profile but are derived from the respective OperationDefinition.

### BrokerProvenance

Profiles `Provenance` with agent slicing: `performer` (organization/site) and `assembler` (connector software). An entity with role `source` documents the local source system via `identifier` and `display`.

### BrokerAuditEvent

Profiles `AuditEvent` with detail slicing for six standardized keys: `operation`, `pseudonym-domain`, `source-system`, `profile-validation`, `result-count`, `duration-ms`. All keys are optional — which ones are set depends on the processing step.
