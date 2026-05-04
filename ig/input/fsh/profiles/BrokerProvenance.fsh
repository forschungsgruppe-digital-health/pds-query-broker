Profile: BrokerProvenance
Parent: Provenance
Id: broker-provenance
Title: "Query Broker Provenance"
Description: """
Profil für Provenance-Ressourcen, die die Herkunft fachlicher Ressourcen
dokumentieren. Agent-Slicing: performer (Organisation) und assembler
(Connector-Software). Entity mit Rolle source für das lokale Quellsystem.
"""

* ^url = "https://querybroker.example.org/fhir/StructureDefinition/BrokerProvenance"
* ^version = "0.1.0"
* ^status = #active

* target 1..* MS
* recorded 1..1

* activity 0..1 MS
* activity from BrokerProvenanceActivityVS (extensible)

* agent ^slicing.discriminator.type = #value
* agent ^slicing.discriminator.path = "type.coding.code"
* agent ^slicing.rules = #open

* agent contains
    performer 1..1 and
    assembler 0..1

* agent[performer].type 1..1
* agent[performer].type.coding 1..1
* agent[performer].type.coding.system = $ProvenanceParticipantType
* agent[performer].type.coding.code = #performer
* agent[performer].who 1..1
* agent[performer].who only Reference(Organization)
* agent[performer].who ^short = "Standort / Organisation, die die Daten bereitstellt"

* agent[assembler].type 1..1
* agent[assembler].type.coding 1..1
* agent[assembler].type.coding.system = $ProvenanceParticipantType
* agent[assembler].type.coding.code = #assembler
* agent[assembler].who 1..1
* agent[assembler].who only Reference(Device)
* agent[assembler].who ^short = "Connector-Software (Name, Version)"

* entity 0..* MS
* entity.role 1..1
* entity.role = #source
* entity.what 1..1
* entity.what.identifier 0..1 MS
* entity.what.identifier ^short = "System-URL und Record-ID im Quellsystem"
* entity.what.display 0..1 MS


ValueSet: BrokerProvenanceActivityVS
Id: broker-provenance-activity-vs
Title: "Query Broker Provenance Activity"
* $DataOperation#CREATE "Create"
* $DataOperation#UPDATE "Update"
* $DataOperation#APPEND "Append"
