# 7. Deployment View

[Back to the architecture docs index](README.md)

> **In brief (for newcomers):** Where the software runs: central infrastructure vs. the per-site PDS networks, and why connections are outbound-only. Terms are defined in the [glossary](12_glossary.md).

```mermaid
graph TB
    subgraph "Central infrastructure"
        BFF_NODE["BFF container"]
        BROKER_NODE["Broker container"]
        MQ_NODE["RabbitMQ container"]
        CAT_NODE["HAPI FHIR catalog"]
    end

    subgraph "PDS-A network"
        CONN_A_NODE["Connector container"]
        THS_A["Local THS"]
        DB_A[("Data system")]
    end

    subgraph "PDS-B network"
        CONN_B_NODE["Connector container"]
        THS_B["Local THS"]
        DB_B[("Data system")]
    end

    BFF_NODE --> BROKER_NODE --> MQ_NODE
    BROKER_NODE --> CAT_NODE
    MQ_NODE ---|"AMQP (outbound from the PDS)"| CONN_A_NODE
    MQ_NODE ---|"AMQP (outbound from the PDS)"| CONN_B_NODE
    CONN_A_NODE --> THS_A --> DB_A
    CONN_B_NODE --> THS_B --> DB_B
```

> PDS connectors establish **outbound** AMQP connections to the central RabbitMQ — no inbound connections into PDS networks are needed. This considerably simplifies firewall configuration in hospital networks.
