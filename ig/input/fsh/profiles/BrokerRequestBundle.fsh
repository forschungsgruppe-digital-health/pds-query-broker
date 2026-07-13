Invariant: qb-request-1
Description: "The MessageHeader of a request bundle must not contain a response element."
Expression: "entry.first().resource.ofType(MessageHeader).response.empty()"
Severity: #error

Profile: BrokerRequestBundle
Parent: Bundle
Id: broker-request-bundle
Title: "Query Broker Request Bundle"
Description: """
Profile for the request message envelope of the Query Broker protocol.

A FHIR message Bundle (`type = message`) whose first entry is a
[BrokerMessageHeader](StructureDefinition-broker-message-header.html) without a
`response` element, followed by exactly one
[BrokerRequestParameters](StructureDefinition-broker-request-parameters.html)
carrying the pseudonyms and operation parameters. Additional entries are
permitted (open slicing) for forward compatibility.
"""

* ^url = "https://querybroker.example.org/fhir/StructureDefinition/BrokerRequestBundle"
* ^version = "0.1.0"
* ^status = #active

* obeys qb-request-1

* type = #message
* timestamp 1..1 MS
* timestamp ^short = "Time the request message was assembled"

* entry 2..*
* entry ^slicing.discriminator.type = #type
* entry ^slicing.discriminator.path = "resource"
* entry ^slicing.rules = #open
* entry ^slicing.description = "MessageHeader and Parameters mandatory; further entries open."

* entry contains
    messageHeader 1..1 and
    parameters 1..1
* entry[messageHeader].resource only BrokerMessageHeader
* entry[messageHeader] ^short = "The message header (first entry, per bdl-4)"
* entry[messageHeader].fullUrl 1..1
* entry[parameters].resource only BrokerRequestParameters
* entry[parameters] ^short = "Pseudonyms and operation parameters"
* entry[parameters].fullUrl 1..1
