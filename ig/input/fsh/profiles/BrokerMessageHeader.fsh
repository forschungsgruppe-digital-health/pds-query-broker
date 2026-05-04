Profile: BrokerMessageHeader
Parent: MessageHeader
Id: broker-message-header
Title: "Query Broker MessageHeader"
Description: """
Profil für den MessageHeader in FHIR Message Bundles des Query Broker Protokolls.

Formalisiert `eventUri` als kanonische OperationDefinition-URL, `destination`
als Response-Routing-Ziel für Multi-Client-Fähigkeit und `source.endpoint`
als AMQP-Adresse. In Antwort-Nachrichten ist `response` verpflichtend.
"""

* ^url = "https://querybroker.example.org/fhir/StructureDefinition/BrokerMessageHeader"
* ^version = "0.1.0"
* ^status = #active

* event[x] only uri
* eventUri 1..1
* eventUri ^short = "Kanonische URL der OperationDefinition"

* definition 0..1 MS
* definition ^short = "Kanonische URL der MessageDefinition"

* destination 1..* MS
* destination.name 0..1 MS
* destination.name ^short = "Name des anfragenden Systems"
* destination.endpoint 1..1
* destination.endpoint ^short = "Response-Queue-URI des anfragenden Systems"
* destination.endpoint ^definition = "AMQP-Queue, auf die der Broker die aggregierte Antwort publiziert. Format: amqp://{host}/responses.{system-id}"

* source 1..1
* source.name 1..1 MS
* source.name ^short = "Name der sendenden Komponente"
* source.endpoint 1..1
* source.endpoint ^short = "AMQP-Endpoint der sendenden Komponente"

* focus 1..* MS
* focus ^short = "Referenzen auf die Payload-Ressourcen"

* response 0..1 MS
* response.identifier 1..1
* response.identifier ^short = "MessageHeader.id der ursprünglichen Request-Nachricht"
* response.code 1..1
* response.code ^short = "ok | transient-error | fatal-error"
