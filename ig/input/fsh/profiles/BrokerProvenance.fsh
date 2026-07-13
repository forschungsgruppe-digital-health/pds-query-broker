Profile: BrokerProvenance
Parent: Provenance
Id: broker-provenance
Title: "Query Broker Provenance"
Description: """
Profile for Provenance resources documenting the origin of clinical
resources. Agent slicing: performer (organization) and assembler
(connector software). Entity with role source for the local source system.
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
* agent[performer].who ^short = "Site / organization providing the data"

* agent[assembler].type 1..1
* agent[assembler].type.coding 1..1
* agent[assembler].type.coding.system = $ProvenanceParticipantType
* agent[assembler].type.coding.code = #assembler
* agent[assembler].who 1..1
* agent[assembler].who only Reference(Device)
* agent[assembler].who ^short = "Connector software (name, version)"

* entity 0..* MS
* entity.role 1..1
* entity.role = #source
* entity.what 1..1
* entity.what.identifier 0..1 MS
* entity.what.identifier ^short = "System URL and record ID in the source system"
* entity.what.display 0..1 MS


ValueSet: BrokerProvenanceActivityVS
Id: broker-provenance-activity-vs
Title: "Query Broker Provenance Activity"
* $DataOperation#CREATE "Create"
* $DataOperation#UPDATE "Update"
* $DataOperation#APPEND "Append"
