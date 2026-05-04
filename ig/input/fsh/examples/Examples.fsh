Instance: ExampleRequestMessageHeader
InstanceOf: BrokerMessageHeader
Usage: #example
Title: "Request MessageHeader"
* eventUri = "https://querybroker.example.org/fhir/OperationDefinition/GetConditions"
* definition = "https://querybroker.example.org/fhir/MessageDefinition/GetConditionsRequest"
* destination[+].name = "Patientenportal"
* destination[=].endpoint = "amqp://rabbitmq.example.org/responses.portal"
* source.name = "Query Broker"
* source.endpoint = "amqp://rabbitmq.example.org/pds.broadcast"
* focus[+].reference = "urn:uuid:params-001"


Instance: ExampleResponseMessageHeader
InstanceOf: BrokerMessageHeader
Usage: #example
Title: "Response MessageHeader"
* eventUri = "https://querybroker.example.org/fhir/OperationDefinition/GetConditions"
* destination[+].name = "Query Broker"
* destination[=].endpoint = "amqp://rabbitmq.example.org/responses.default"
* source.name = "PDS-A Connector"
* source.endpoint = "amqp://rabbitmq.example.org/req.PDS-A"
* response.identifier = "msg-header-001"
* response.code = #ok
* focus[+].reference = "urn:uuid:condition-001"


Instance: ExampleRequestParameters
InstanceOf: BrokerRequestParameters
Usage: #example
Title: "Request Parameters"
* parameter[pseudonym][+].name = "pseudonym"
* parameter[pseudonym][=].valueIdentifier.system = "https://ths.example.org/gpas/domain/PDS-A"
* parameter[pseudonym][=].valueIdentifier.value = "PSN-A-8x3k"
* parameter[pseudonym][+].name = "pseudonym"
* parameter[pseudonym][=].valueIdentifier.system = "https://ths.example.org/gpas/domain/PDS-B"
* parameter[pseudonym][=].valueIdentifier.value = "PSN-B-m9zq"


Instance: ExampleProvenance
InstanceOf: BrokerProvenance
Usage: #example
Title: "Provenance"
* target[+].reference = "urn:uuid:condition-001"
* recorded = "2026-05-01T10:15:02Z"
* activity = $DataOperation#CREATE
* agent[performer].type.coding[+] = $ProvenanceParticipantType#performer
* agent[performer].who = Reference(Organization/pds-a) "PDS-A"
* agent[assembler].type.coding[+] = $ProvenanceParticipantType#assembler
* agent[assembler].who = Reference(Device/pds-a-connector) "PDS-A Connector v2.1.0"
* entity[+].role = #source
* entity[=].what.identifier.system = "https://pds-a.example.org/systems/local"
* entity[=].what.identifier.value = "condition/48291"
* entity[=].what.display = "Lokales Datensystem"


Instance: ExampleAuditEvent
InstanceOf: BrokerAuditEvent
Usage: #example
Title: "AuditEvent"
* type = http://dicom.nema.org/resources/ontology/DCM#110112 "Query"
* action = #E
* period.start = "2026-05-01T10:15:00.000Z"
* period.end = "2026-05-01T10:15:02.341Z"
* recorded = "2026-05-01T10:15:02.341Z"
* outcome = #0
* agent[+].type = $SecurityRoleType#dataprocessor
* agent[=].who = Reference(Device/pds-a-connector) "PDS-A Connector"
* agent[=].requestor = false
* source.observer = Reference(Device/pds-a-connector)
* source.type[+] = $AuditSourceType#4 "Application Server"
* entity[+].what.reference = "urn:uuid:condition-001"
* entity[=].type = $AuditEntityType#2 "System Object"
* entity[=].detail[operation].type = "operation"
* entity[=].detail[operation].valueString = "GetConditions"
* entity[=].detail[resultCount].type = "result-count"
* entity[=].detail[resultCount].valueString = "3"
* entity[=].detail[durationMs].type = "duration-ms"
* entity[=].detail[durationMs].valueString = "2341"
