Profile: BrokerRequestParameters
Parent: Parameters
Id: broker-request-parameters
Title: "Query Broker Request Parameters"
Description: """
Profile for the Parameters resource in request messages. Formalizes the
pseudonym parameter as a repeatable Identifier. Further operation-specific
parameters are open (open slicing).
"""

* ^url = "https://querybroker.example.org/fhir/StructureDefinition/BrokerRequestParameters"
* ^version = "0.1.0"
* ^status = #active

* parameter ^slicing.discriminator.type = #value
* parameter ^slicing.discriminator.path = "name"
* parameter ^slicing.rules = #open
* parameter ^slicing.description = "Pseudonym parameter mandatory, operation-specific parameters open."

* parameter contains pseudonym 1..*
* parameter[pseudonym].name = "pseudonym"
* parameter[pseudonym].value[x] only Identifier
* parameter[pseudonym].valueIdentifier 1..1
* parameter[pseudonym].valueIdentifier.system 1..1
* parameter[pseudonym].valueIdentifier.system ^short = "URI of the pseudonymization domain"
* parameter[pseudonym].valueIdentifier.value 1..1
* parameter[pseudonym].valueIdentifier.value ^short = "The site-specific pseudonym"
