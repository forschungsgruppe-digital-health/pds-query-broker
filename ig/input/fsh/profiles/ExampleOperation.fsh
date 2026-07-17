Instance: GetConditions
InstanceOf: OperationDefinition
Usage: #definition
* url = "https://querybroker.example.org/fhir/OperationDefinition/GetConditions"
* name = "GetConditions"
* title = "Retrieve diagnoses (example operation)"
* status = #active
* kind = #operation
* code = #GetConditions
* description = "Exemplary operation: retrieves diagnoses. For the pilot, the output is bound to the MII KDS Diagnose profile via targetProfile (ADR-012); the binding mechanism itself remains optional and project-specific."
* resource = #Condition
* system = false
* type = true
* instance = false
* parameter[+].name = #pseudonym
* parameter[=].use = #in
* parameter[=].min = 1
* parameter[=].max = "*"
* parameter[=].type = #Identifier
* parameter[+].name = #dateFrom
* parameter[=].use = #in
* parameter[=].min = 0
* parameter[=].max = "1"
* parameter[=].type = #date
* parameter[+].name = #code
* parameter[=].use = #in
* parameter[=].min = 0
* parameter[=].max = "1"
* parameter[=].type = #string
* parameter[+].name = #return
* parameter[=].use = #out
* parameter[=].min = 1
* parameter[=].max = "1"
* parameter[=].type = #Bundle
* parameter[=].part[+].name = #condition
* parameter[=].part[=].use = #out
* parameter[=].part[=].min = 0
* parameter[=].part[=].max = "*"
* parameter[=].part[=].type = #Condition
* parameter[=].part[=].targetProfile = "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose"


Instance: GetConditionsRequest
InstanceOf: MessageDefinition
Usage: #definition
* url = "https://querybroker.example.org/fhir/MessageDefinition/GetConditionsRequest"
* name = "GetConditionsRequest"
* title = "GetConditions — Request"
* status = #active
* date = "2026-05-01"
* eventUri = "https://querybroker.example.org/fhir/OperationDefinition/GetConditions"
* category = #consequence
* responseRequired = #always
* focus[+].code = #Parameters
* focus[=].profile = "https://querybroker.example.org/fhir/StructureDefinition/BrokerRequestParameters"
* focus[=].min = 1
* focus[=].max = "1"
* allowedResponse[+].message = "https://querybroker.example.org/fhir/MessageDefinition/GetConditionsResponse"
* allowedResponse[+].message = "https://querybroker.example.org/fhir/MessageDefinition/OperationError"


Instance: GetConditionsResponse
InstanceOf: MessageDefinition
Usage: #definition
* url = "https://querybroker.example.org/fhir/MessageDefinition/GetConditionsResponse"
* name = "GetConditionsResponse"
* title = "GetConditions — Response"
* status = #active
* date = "2026-05-01"
* eventUri = "https://querybroker.example.org/fhir/OperationDefinition/GetConditions"
* category = #consequence
* focus[+].code = #Condition
* focus[=].profile = "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose"
* focus[=].min = 0
* focus[=].max = "*"


Instance: GetConditionsResponseGraph
InstanceOf: GraphDefinition
Usage: #definition
* url = "https://querybroker.example.org/fhir/GraphDefinition/GetConditionsResponseGraph"
* name = "GetConditionsResponseGraph"
* status = #active
* start = #Bundle
* link[+].path = "Bundle.entry.resource"
* link[=].target[+].type = #Condition
* link[=].target[=].profile = "https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose"
