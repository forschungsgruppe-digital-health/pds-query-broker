# Security-review focus — Query Broker (FHIR messaging over AMQP)

This is a federated FHIR R4 **query broker**: a portal/BFF submits FHIR Message Bundles; the
broker fans them out over RabbitMQ (AMQP) to per-site connectors, which resolve **pseudonyms**
via a trusted third party (THS/gPAS) and return FHIR resources. Patient identities are
pseudonymized; there is no central data store. Weight findings by real exploitability for this
architecture; prefer precise, mechanism-level findings over generic advice.

## Prioritize these classes

- **Authorization / tenancy isolation** — cross-client response leakage. Responses are routed by
  `MessageHeader.destination.endpoint` + AMQP `replyTo`; verify a client cannot read another
  client's aggregated Bundle (queue-steering, correlation-id collisions, missing/again-usable
  `responses.{systemId}` isolation).
- **Pseudonym / PII handling** — pseudonyms or re-identifying data leaking into logs, error
  messages, `OperationOutcome`, `AuditEvent` detail, or across site boundaries; each addressed
  site must not receive other sites' pseudonyms — the broker trims each topic-mode request to the
  site's own pseudonym(s) (ADR-006 rev., `BrokerMessages.requestBundleForSite`). Verify no
  cross-site pseudonym in a published bundle (legacy fanout mode still broadcasts all pseudonyms —
  connector self-filtering is the safety net there).
- **THS / gPAS integration** — SSRF or injection via `dispatcher-base-url` / `target-domain`;
  trusting unauthenticated de-pseudonymization responses; TLS/mTLS verification on the terminology
  and THS clients (certs must never be logged or committed).
- **FHIR message handling** — unsafe parsing (XXE/entity expansion in XML FHIR), resource-type
  confusion, `eventUri`/profile spoofing, oversized/poison messages, missing size/timeout limits,
  DoS via fan-out amplification.
- **AMQP/broker** — unauthenticated publish/subscribe, missing per-service credentials/vhost,
  poison-message requeue loops, dead-letter handling, message replay/injection.
- **Secrets & config** — credentials, keystores, or tokens in code/logs/tests; `.env` vs
  `.env.example`; the RabbitMQ dev-default password hash is a documented allowlisted test value.
- **Injection & crypto** — command/log injection, weak TLS/keystore handling, insecure randomness
  for identifiers/correlation.

## De-prioritize (context)

- The dev stack is intentionally unauthenticated for the walking skeleton; do not report the
  absence of end-to-end auth as a new vulnerability — it is a documented, staged decision
  (see `docs/arc42/` and the ADRs). Flag only concrete regressions or exploitable gaps.
- Synthetic test data with obviously artificial names is expected and not a finding.

Every finding is a **lead for human confirmation**, never a verdict.
