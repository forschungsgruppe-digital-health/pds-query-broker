Profile: BrokerRequestParameters
Parent: Parameters
Id: broker-request-parameters
Title: "Query Broker Request Parameters"
Description: """
Profil für die Parameters-Ressource in Request-Nachrichten. Formalisiert den
Pseudonym-Parameter als wiederholbaren Identifier. Weitere operationsspezifische
Parameter sind offen (Open Slicing).
"""

* ^url = "https://querybroker.example.org/fhir/StructureDefinition/BrokerRequestParameters"
* ^version = "0.1.0"
* ^status = #active

* parameter ^slicing.discriminator.type = #value
* parameter ^slicing.discriminator.path = "name"
* parameter ^slicing.rules = #open
* parameter ^slicing.description = "Pseudonym-Parameter verpflichtend, operationsspezifische Parameter offen."

* parameter contains pseudonym 1..*
* parameter[pseudonym].name = "pseudonym"
* parameter[pseudonym].value[x] only Identifier
* parameter[pseudonym].valueIdentifier 1..1
* parameter[pseudonym].valueIdentifier.system 1..1
* parameter[pseudonym].valueIdentifier.system ^short = "URI der Pseudonymisierungsdomäne"
* parameter[pseudonym].valueIdentifier.value 1..1
* parameter[pseudonym].valueIdentifier.value ^short = "Das standortspezifische Pseudonym"
