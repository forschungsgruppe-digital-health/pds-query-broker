This IG contains an exemplary operation (`GetConditions`) as a template for project-specific operations. Each operation consists of a triple:

1. **OperationDefinition** — semantics: parameters, types, cardinalities, optional profile binding (`targetProfile`)
2. **MessageDefinition** — message contract: mandatory payloads, allowed responses, profile binding (`focus.profile`)
3. **GraphDefinition** — payload structure: resource graph, profile binding (`target.profile`)

### Generic error response: OperationError

In addition to its operation-specific response, every request MessageDefinition lists the generic [OperationError](MessageDefinition-OperationError.html) MessageDefinition in `allowedResponse`. It is sent when a request fails as a whole (event URI `https://querybroker.example.org/fhir/event/operation-error`, `response.code = fatal-error`) and carries one or more `BrokerOperationOutcome` resources with machine-readable error codes. Partial failures of an otherwise successful aggregation travel as additional `OperationOutcome` entries inside the regular response message instead.

### Defining project-specific operations

New operations are created as FHIR resources in the message catalog — not as code changes to the broker or to existing connectors. The profile binding via `targetProfile`, `focus.profile`, and `target.profile` is optional: operations without a profile binding return base FHIR resources.

Examples of possible profile bindings:

- MII Core Data Set (MII KDS) in the MII context
- US Core for US-based projects
- International Patient Summary (IPS) for international scenarios
- Custom project profiles
- No profile binding (base FHIR resources)
