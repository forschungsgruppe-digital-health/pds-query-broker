CodeSystem: BrokerErrorCodes
Id: broker-error-codes
Title: "Query Broker Error Codes"
Description: """
Machine-readable error codes of the Query Broker protocol, used in
`OperationOutcome.issue.details`. They refine the generic FHIR
`issue.code` so that consumers (e.g. the BFF) can react programmatically.
"""
* ^url = "https://querybroker.example.org/fhir/CodeSystem/BrokerErrorCodes"
* ^version = "0.1.0"
* ^status = #active
* ^caseSensitive = true
* ^content = #complete
* #timeout "PDS response timeout" "A PDS did not respond within the configured time window; the aggregated result is potentially incomplete. Corresponds to issue.code 'timeout'."
* #unsupported-operation "Operation not supported" "The addressed PDS does not support the requested operation (not listed in its CapabilityStatement). Corresponds to issue.code 'not-supported'."
* #no-capable-pds "No capable PDS" "No PDS site declared the requested operation; the broker cannot deliver any result. Corresponds to issue.code 'not-found'."
* #pds-error "PDS-side processing error" "The connector or the local data system failed while processing the request. Corresponds to issue.code 'exception'."
* #validation-error "Profile validation failed" "A produced resource did not conform to the configured targetProfile and was rejected before sending. Corresponds to issue.code 'invalid'."

ValueSet: BrokerErrorCodesVS
Id: broker-error-codes-vs
Title: "Query Broker Error Codes ValueSet"
Description: "All error codes of the Query Broker protocol."
* ^url = "https://querybroker.example.org/fhir/ValueSet/BrokerErrorCodesVS"
* ^version = "0.1.0"
* ^status = #active
* include codes from system BrokerErrorCodes

Profile: BrokerOperationOutcome
Parent: OperationOutcome
Id: broker-operation-outcome
Title: "Query Broker OperationOutcome"
Description: """
Profile for the error model of the Query Broker protocol (see
ARCHITECTURE.md § 8.6). Every issue carries a machine-readable broker error
code in `issue.details` (extensible binding to
[BrokerErrorCodesVS](ValueSet-broker-error-codes-vs.html)) in addition to the
generic FHIR `issue.code`, plus human-readable diagnostics. Emitted by the
broker (timeout, no capable PDS) and by connectors (unsupported operation,
PDS-side errors, validation failures); transported as an entry of a
[BrokerResponseBundle](StructureDefinition-broker-response-bundle.html).
"""

* ^url = "https://querybroker.example.org/fhir/StructureDefinition/BrokerOperationOutcome"
* ^version = "0.1.0"
* ^status = #active

* issue 1..*
* issue.severity MS
* issue.severity ^short = "fatal | error | warning | information"
* issue.code MS
* issue.code ^short = "Generic FHIR issue type (e.g. timeout, not-supported, exception, invalid)"
* issue.details 1..1 MS
* issue.details from BrokerErrorCodesVS (extensible)
* issue.details ^short = "Machine-readable broker error code"
* issue.details.coding 1..*
* issue.diagnostics 0..1 MS
* issue.diagnostics ^short = "Human-readable detail, e.g. affected PDS ID and timeout value"
