---
name: fhir-ig
description: FHIR R4 profiling with FHIR Shorthand (FSH) and SUSHI for the Query Broker ImplementationGuide — profiles, operations (triples), examples, page content. Use for any change under ig/ or to the catalog artifacts derived from it.
---

# Skill: FHIR IG profiling and definition

You are a specialist for FHIR R4 profiling with FHIR Shorthand (FSH) and SUSHI.

## Working set

- FSH files: `ig/input/fsh/profiles/` and `ig/input/fsh/examples/`
- Configuration: `ig/sushi-config.yaml`
- IG pages: `ig/input/pagecontent/`

## Existing profiles

- `BrokerMessageHeader` — MessageHeader with destination, eventUri, response
- `BrokerRequestParameters` — Parameters with pseudonym slicing (open slicing)
- `BrokerProvenance` — Provenance with agent slicing (performer/assembler)
- `BrokerAuditEvent` — AuditEvent with detail slicing (6 keys)
- `ExampleOperation.fsh` — GetConditions as the exemplary triple

## Rules

1. **FSH is the single source of truth** — never author or edit FHIR conformance JSON by hand.
   `catalog/` contains only generated mirrors: after every successful build, run
   `python3 ig/scripts/mirror-catalog.py` from the repo root and commit the result
   (CI fails on any catalog/FSH drift)
2. **Always compile** after changes: `cd ig && sushi build` — target: 0 errors, 0 warnings
3. **Profile binding is optional** — never declare `targetProfile`, `focus.profile`, `target.profile` as mandatory
4. **OperationDefinition names**: PascalCase; use the `code` field (not `name` as a string)
5. **MessageDefinition**: `date` is mandatory; `eventUri` references the OperationDefinition
6. **New profiles**: create in `ig/input/fsh/profiles/`, example instance in `ig/input/fsh/examples/`
7. **Update page content**: register new profiles/operations on the corresponding .md pages
8. **Aliases**: define shared CodeSystem URLs in `ig/input/fsh/aliases.fsh`
9. **Dependencies**: external profiles (MII KDS, US Core, etc.) as dependencies in `sushi-config.yaml`
10. **No colors** in Mermaid diagrams

## Typical tasks

- "Create a profile for [resource]" → FSH file + example instance + page content
- "Define a new operation [name]" → OperationDefinition + MessageDefinition(s) + GraphDefinition
- "Add a binding to [CodeSystem]" → ValueSet + binding in the profile
- "Extend [profile] by [element]" → constraint in FSH, update the example
