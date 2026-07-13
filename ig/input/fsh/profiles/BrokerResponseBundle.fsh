Invariant: qb-response-1
Description: "The MessageHeader of a response bundle must contain a response element with the correlation identifier of the request."
Expression: "entry.first().resource.ofType(MessageHeader).response.exists()"
Severity: #error

Profile: BrokerResponseBundle
Parent: Bundle
Id: broker-response-bundle
Title: "Query Broker Response Bundle"
Description: """
Profile for the response message envelope of the Query Broker protocol —
used both for a single connector response and for the aggregated response
the broker returns to the requesting system.

A FHIR message Bundle (`type = message`) whose first entry is a
[BrokerMessageHeader](StructureDefinition-broker-message-header.html) with a
mandatory `response` element (correlation identifier + `ok | transient-error |
fatal-error`). Result resources (e.g. `Condition`) travel as further entries —
their type and optional profile binding follow from the operation's
GraphDefinition and `targetProfile`, so they are deliberately NOT constrained
here (open slicing). Errors are transported as
[BrokerOperationOutcome](StructureDefinition-broker-operation-outcome.html)
entries; data provenance and processing logs as
[BrokerProvenance](StructureDefinition-broker-provenance.html) and
[BrokerAuditEvent](StructureDefinition-broker-audit-event.html) entries.
"""

* ^url = "https://querybroker.example.org/fhir/StructureDefinition/BrokerResponseBundle"
* ^version = "0.1.0"
* ^status = #active

* obeys qb-response-1

* type = #message
* timestamp 1..1 MS
* timestamp ^short = "Time the response message was assembled"

* entry 1..*
* entry ^slicing.discriminator.type = #type
* entry ^slicing.discriminator.path = "resource"
* entry ^slicing.rules = #open
* entry ^slicing.description = "MessageHeader mandatory; results open; errors/provenance/audit typed."

* entry contains
    messageHeader 1..1 and
    operationOutcome 0..* and
    provenance 0..* and
    auditEvent 0..*
* entry[messageHeader].resource only BrokerMessageHeader
* entry[messageHeader] ^short = "The message header (first entry, per bdl-4) with mandatory response element"
* entry[messageHeader].fullUrl 1..1
* entry[operationOutcome].resource only BrokerOperationOutcome
* entry[operationOutcome] ^short = "Errors and warnings (timeout, unsupported operation, PDS-side errors)"
* entry[provenance].resource only BrokerProvenance
* entry[provenance] ^short = "Per-resource data provenance"
* entry[auditEvent].resource only BrokerAuditEvent
* entry[auditEvent] ^short = "Processing log entries"
