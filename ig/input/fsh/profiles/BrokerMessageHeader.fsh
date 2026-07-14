Profile: BrokerMessageHeader
Parent: MessageHeader
Id: broker-message-header
Title: "Query Broker MessageHeader"
Description: """
Profile for the MessageHeader in FHIR message Bundles of the Query Broker protocol.

Formalizes `eventUri` as the canonical OperationDefinition URL, `destination`
as the response-routing target for multi-client capability, and `source.endpoint`
as the AMQP address. In response messages, `response` is mandatory.
"""

* ^url = "https://querybroker.example.org/fhir/StructureDefinition/BrokerMessageHeader"
* ^version = "0.1.0"
* ^status = #active

* event[x] only uri
* eventUri 1..1
* eventUri ^short = "Canonical URL of the OperationDefinition"

* definition 0..1 MS
* definition ^short = "Canonical URL of the MessageDefinition"

* destination 1..* MS
* destination.name 0..1 MS
* destination.name ^short = "Name of the requesting system"
* destination.endpoint 1..1
* destination.endpoint ^short = "Response queue URI of the requesting system"
* destination.endpoint ^definition = "AMQP queue to which the broker publishes the aggregated response. Format: amqp://{host}/responses.{system-id}"

* source 1..1
* source.name 1..1 MS
* source.name ^short = "Name of the sending component"
* source.endpoint 1..1
* source.endpoint ^short = "AMQP endpoint of the sending component"

* focus 0..* MS
* focus ^short = "References to the payload resources (absent in empty-result responses)"

* response 0..1 MS
* response.identifier 1..1
* response.identifier ^short = "MessageHeader.id of the original request message"
* response.code 1..1
* response.code ^short = "ok | transient-error | fatal-error"
