Diese Seite beschreibt die vier Infrastrukturprofile des Query Broker Protokolls.

### BrokerMessageHeader

Profiliert `MessageHeader` für FHIR Messaging über AMQP. Verpflichtet `eventUri` (OperationDefinition-URL), `destination` (Response-Routing für Multi-Client-Fähigkeit) und `source` (AMQP-Endpoint). In Antwort-Nachrichten ist `response` mit Korrelations-ID und Status-Code verpflichtend.

### BrokerRequestParameters

Profiliert `Parameters` mit Open Slicing. Der Slice `pseudonym` (1..*) ist ein typisierter `Identifier` mit `system` = Pseudonymisierungsdomäne und `value` = Pseudonym. Operationsspezifische Parameter werden durch `#open` Slicing unterstützt — sie müssen nicht im Profil deklariert werden, sondern ergeben sich aus der jeweiligen OperationDefinition.

### BrokerProvenance

Profiliert `Provenance` mit Agent-Slicing: `performer` (Organisation/Standort) und `assembler` (Connector-Software). Entity mit Rolle `source` dokumentiert das lokale Quellsystem über `identifier` und `display`.

### BrokerAuditEvent

Profiliert `AuditEvent` mit Detail-Slicing für sechs standardisierte Keys: `operation`, `pseudonym-domain`, `source-system`, `profile-validation`, `result-count`, `duration-ms`. Alle Keys sind optional — welche gesetzt werden, hängt vom Verarbeitungsschritt ab.
