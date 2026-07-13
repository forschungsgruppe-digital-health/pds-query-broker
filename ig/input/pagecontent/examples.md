This page lists the FHIR example instances of this ImplementationGuide.

### Messaging

- [Request Bundle](Bundle-ExampleRequestBundle.html) — Complete request envelope: MessageHeader + Parameters
- [Error Response Bundle](Bundle-ExampleErrorResponseBundle.html) — OperationError response: MessageHeader (`response.code = fatal-error`) + OperationOutcome
- [Request MessageHeader](MessageHeader-ExampleRequestMessageHeader.html) — Request message with `destination` for response routing
- [Response MessageHeader](MessageHeader-ExampleResponseMessageHeader.html) — Successful connector response with `response.code = ok`
- [Error MessageHeader](MessageHeader-ExampleErrorMessageHeader.html) — Error response header referencing the OperationError MessageDefinition
- [Request Parameters](Parameters-ExampleRequestParameters.html) — Two site-specific pseudonyms and an optional date filter
- [OperationOutcome (timeout)](OperationOutcome-ExampleTimeoutOutcome.html) — Machine-readable broker error code `timeout`

### Provenance and logging

- [Provenance](Provenance-ExampleProvenance.html) — Provenance documentation: organization, connector, source system
- [AuditEvent](AuditEvent-ExampleAuditEvent.html) — Processing log with all detail keys
