Dieser IG enthält eine exemplarische Operation (`GetConditions`) als Vorlage für projektspezifische Operationen. Jede Operation besteht aus einem Tripel:

1. **OperationDefinition** — Semantik: Parameter, Typen, Kardinalitäten, optionale Profilbindung (`targetProfile`)
2. **MessageDefinition** — Nachrichtenvertrag: Pflicht-Payloads, erlaubte Antworten, Profilbindung (`focus.profile`)
3. **GraphDefinition** — Payload-Struktur: Ressourcengraph, Profilbindung (`target.profile`)

### Projektspezifische Operationen definieren

Neue Operationen werden als FHIR-Ressourcen im Nachrichtenkatalog angelegt — nicht als Code-Änderung am Broker oder an bestehenden Connectoren. Die Profilbindung über `targetProfile`, `focus.profile` und `target.profile` ist optional: Operationen ohne Profilbindung liefern FHIR-Basisressourcen zurück.

Beispiele für mögliche Profilbindungen:

- MII-Kerndatensatz (MII KDS) im MII-Kontext
- US Core für US-amerikanische Projekte
- International Patient Summary (IPS) für internationale Szenarien
- Eigene Projektprofile
- Keine Profilbindung (FHIR-Basisressourcen)
