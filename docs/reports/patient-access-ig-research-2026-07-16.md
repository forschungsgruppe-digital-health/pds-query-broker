# Patient-Access FHIR IGs — Research & Fit Analysis

> Dated research report, 2026-07-16 (immutable snapshot — supersede with a newer dated report, do not edit).
> Method: 5 parallel web-research passes (one per IG cluster, primary sources fetched 2026-07-16) + an
> independent fact-verification pass re-fetching every version/status claim (19/20 CONFIRMED; 1 target
> unreachable [FDPG website, not an IG]; 1 CI-build detail corrected).
> Decision derived from this report: [ADR-012](../ARCHITECTURE.md#adr-012-content-profile-strategy--mii-kds-first-ipa-aligned-facade-boundary).
>
> Question: which European/international FHIR IGs address patients' access to their own data, which are in
> scope for this repo, and how could they realize data exchange between the patient portal and the data
> integration centers (DIC — the repo's "primary data source (PDS)" sites) over the broker/messaging approach?

## 1. Landscape (verified 2026-07-16)

| IG | Version / status | Paradigm | Essence |
|----|------------------|----------|---------|
| **HL7 International Patient Access (IPA)** — `hl7.fhir.uv.ipa` | **1.1.0, STU1**, 2025-03-19 | SMART-on-FHIR REST, read/search only | *The* universal baseline for apps acting on behalf of a patient: minimum-expectation profiles (Patient, Condition, Observation, MedicationRequest/Statement, AllergyIntolerance, Immunization, DocumentReference, …), mandatory interactions + search parameters, `CapabilityStatement.instantiates = ipa-server`. Realm IGs re-profile on top. |
| **EU Health Data API** (HL7 Europe + IHE Europe, EURIDICE initiative, Xt-EHR support) — `hl7.fhir.eu.health-data-api` | **1.0.0-ballot, STU1 Ballot**, 2026-03-13, status *draft* | REST: **IPA** for resources (QEDm-aligned) + **IHE MHD** for documents; OAuth2 / SMART Backend Services | Designated candidate spec for the **EHDS Art. 15 / Annex II** EHR-system interoperability component (EEHRxF). **Hard dependency on IPA 1.1.x.** Six actors (Resource/Document Access Provider, Consumer, Publisher). Profiles almost nothing itself (one MHD DocumentReference). Patient access modeled as a trusted national **"Health Data Access Service"** (system-to-system after eID login); user-level SMART App Launch explicitly out of scope in this ballot. |
| **International Patient Summary (IPS)** — `hl7.fhir.uv.ips` | **2.0.1, STU2**, 2026-06-19 | FHIR document + `Patient/$summary` | Universal patient-summary document; since 2.x formally **depends on IPA 1.1.0** — the global standard itself couples summary content to the patient-access API. |
| **HL7 Europe Patient Summary (EPS)** — `hl7.fhir.eu.eps` | 1.0.0-ballot, 2026-06-06 | FHIR document (IPS-derived, obligation-only profiles) | The EEHRxF / MyHealth@EU summary content model; EHDS priority category 1. |
| **HL7 Europe Laboratory Report** — `hl7.fhir.eu.laboratory` | **2.0.0, STU2**, 2026-05-05 (most mature EU content IG) | **Dual**: signable document bundle *and* REST perspective (`DiagnosticReportLabEu`) | EHDS "medical test results" category. |
| **HL7 Europe HDR / MPD** | HDR 0.1.0-ballot (2025-06-03); MPD 1.0.0 STU1 (2026-05-11) | Document / workflow-REST | Hospital discharge report; medication prescription & dispense (EU HDAA explicitly delegates ePrescription workflow to IHE MPD). |
| **gematik "ePA für alle"** — `de.gematik.epa` 1.3.2, `.medication` 1.3.5, `.mhd` 1.1.0 | Production, all active as of 2026-07 | Hybrid: XDS.b writes + IHE MHD (ITI-67/68) reads + FHIR data services; **no SMART App Launch anywhere** | Germany's statutory national patient-access rail (opt-out, KVNR-keyed, audit-logged). |
| **gematik ISiK Basismodul** — `de.gematik.isik-basismodul` | Stufe 5 (5.1.2), active 2026-04-30; mandatory deadline not yet announced | Plain FHIR REST | In-hospital KIS API (§373 SGB V); no patient actor. |
| **MII Kerndatensatz (KDS)** — `de.medizininformatikinitiative.kerndatensatz.*` | person/diagnose/prozedur **2025.0.1**; laborbefund **2026.0.3**; medikation **2026.0.1**; consent 2026.0.0 | FHIR R4 profiles + research-repository search; **not** an app-facing access API | **What the DICs natively hold.** No MII patient-access IG exists; FDPG is researcher-facing only — a patient portal at a DIC fills a gap rather than duplicating an MII spec. |
| **IHE QEDm 3.0.0 / MHD 4.2.4 / PIXm 3.1.0 / PDQm 3.2.0** | Trial Implementation (2024–2026) | Synchronous FHIR REST | The building blocks the EU HDAA composes; MHD is explicitly a REST facade usable over federated XDS/XCA backends. |
| **MedMij (NL), Kanta PHR (FI), CARIN Blue Button (US)** | Production / development-frozen / STU2 2.2.0 | All synchronous REST + OAuth2 | National comparators. Kanta is frozen *because "the EHDS regulation requires modifications"* — a warning to stay on the convergence path. MedMij's DVP/DVZA "brokers" broker **trust**, not transport. |

**Decisive comparator finding:** across everything fetched, **no patient-access IG uses FHIR messaging or
broker-style asynchronous federation for the patient-facing interface** — all prescribe synchronous REST with
OAuth2/SMART-family authorization. The nearest architectural precedents to this repo are (a) **IHE MHD as a
synchronous REST facade over federated XDS/XCA** (federation hidden behind the API) and (b) the Dutch
"notified pull" (async notify → synchronous pull), which explicitly rejects FHIR messaging and lives in
provider-to-provider exchange, not patient access.

## 2. Fit analysis against this repo

The repo separates **transport** (AMQP/AsyncAPI), **operation semantics** (the OperationDefinition +
MessageDefinition + GraphDefinition triple) and **content profiles** (`targetProfile` / `focus.profile` /
`target.profile`, deliberately project-selectable). Patient-access IGs split along exactly that seam:

- **Access-mechanics IGs (IPA, EU HDAA, QEDm) are not implementable *by the broker*** — they prescribe a
  synchronous REST server actor. They belong at the **BFF boundary** (portal repo, ADR-011): the BFF becomes
  the IPA-server-shaped facade and the broker federates fulfillment behind it — the same pattern as
  MHD-over-XDS, so there is standards-track precedent for "synchronous facade, federated backend". The broker
  itself can never claim IPA conformance; **the system (portal + BFF + broker + DICs) can.**
- **Content IGs (MII KDS, IPA profiles, IPS/EPS, EU Lab) are directly usable today** through the triple's
  binding points, enforced end-to-end by existing machinery: SDK `targetProfile` validation, the conformance
  harness, and the catalog.
- The EU HDAA ballot's patient-access model (trusted **Health Data Access Service**, system-to-system after
  eID login, SMART App Launch out of scope) matches this project's BFF pattern (Keycloak-authenticated portal,
  BFF as sole entry) — the EHDS-track spec endorses the architecture already chosen.

| IG | In scope? | Role for this repo |
|----|-----------|--------------------|
| MII KDS | ✅ yes — first | DIC-facing content profiles bound via `targetProfile` (starting: GetConditions → KDS Diagnose) |
| HL7 IPA 1.1.0 | ✅ yes — dual role | (a) conformance target for the future BFF facade; (b) minimum content bar validated on broker outputs |
| EU Health Data API | 🟡 track & align | Ballot aimed at EHDS Implementing Acts; hard-depends on IPA → IPA-shaped now makes HDAA additive later |
| IPS 2.0.1 / EU EPS | 🟡 future operation payload | `GetPatientSummary` operation returning an IPS document bundle; EPS = EU tightening to watch |
| HL7 EU Laboratory Report | 🟡 future mapping layer | KDS Laborbefund is the DIC-native source; EU Lab = EHDS export shape |
| gematik ePA | ❌ not a target | Complementary national rail; boundary to respect, not duplicate |
| gematik ISiK | ❌ portal-side / 🟡 connector-side | Potential *source* API for a hospital connector |
| IHE QEDm/MHD/PIXm/PDQm | 🟡 reference | QEDm≈IPA alignment; MHD = facade precedent; PIXm/PDQm = standards analog of the gPAS/E-PIX TTP layer (TTP kept) |
| MedMij / Kanta / CARIN BB | ❌ | Comparators only |

## 3. Suggested integration path

- **S1 — bind MII KDS to the existing operation** (`GetConditions` → KDS Diagnose Condition profile; KDS
  package as IG dependency; validator gains FHIR-package support; synthetic data made conformant; harness
  proves it). → realized together with ADR-012.
- **S2 — IPA as minimum content bar + operation roadmap**: grow the catalog toward IPA's mandatory resource
  set (one triple per resource); harness validates outputs against both KDS (DIC-facing) and IPA
  (portal-facing) profiles.
- **S3 — BFF as the IPA-shaped facade** (portal repo, with the planned BFF increment): read-only IPA-style
  REST, Keycloak-secured (SMART App Launch deferred — consistent with the EU HDAA stance), translating IPA
  search interactions into broker operations; claim `CapabilityStatement.instantiates` only when the
  interaction set is complete. DPIA touchpoint when implemented.
- **S4 — document/summary stack**: `GetPatientSummary` (IPS bundle focus); `GetLabResults` (KDS Laborbefund
  now, EU Lab Report mapping later); MHD-style document access only if DICs expose documents.
- **S5 — governance**: ADR-012; watch-list for EU HDAA + EPS ballots; re-evaluate at HDAA 1.0.0 final.
  Conformance language everywhere: *"IPA-aligned content over a federated messaging backbone, with the
  portal-facing facade as the conformance boundary"* — never "the broker is IPA-conformant".

## 4. Key sources (fetched 2026-07-16)

- IPA: https://hl7.org/fhir/uv/ipa · https://hl7.org/fhir/uv/ipa/CapabilityStatement-ipa-server.html · https://build.fhir.org/ig/HL7/fhir-ipa/
- EU Health Data API: https://hl7.eu/fhir/health-data-api/1.0.0-ballot/en · https://euridice.org/eu-health-data-api/
- IPS: https://hl7.org/fhir/uv/ips · EPS: https://hl7.eu/fhir/eps/1.0.0-ballot/ · Lab: https://hl7.eu/fhir/laboratory/ · HDR: https://hl7.eu/fhir/hdr/ · MPD: https://hl7.eu/fhir/mpd/
- gematik: https://gemspec.gematik.de/ig/fhir/epa/ · …/epa-medication/ · …/epa-mhd/1.1.0/ · ISiK: https://simplifier.net/guide/isik-basis-stufe-5?version=5.1.2
- MII KDS: https://simplifier.net/organization/koordinationsstellemii (packages `de.medizininformatikinitiative.kerndatensatz.*`)
- IHE: https://profiles.ihe.net/PCC/QEDm/ · https://profiles.ihe.net/ITI/MHD/ · /ITI/PIXm/ · /ITI/PDQm/
- Comparators: https://informatiestandaarden.nictiz.nl/wiki/MedMij:V2019.01_FHIR_IG · https://simplifier.net/guide/finnishphrimplementationguidestu5/ · https://hl7.org/fhir/us/carin-bb/
