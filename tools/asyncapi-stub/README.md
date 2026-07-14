# AsyncAPI contract test (Python stub)

Proves that `specs/pds-connector-base.yaml` is the contract the running system
actually speaks — by building a **mock Python PDS connector from the spec
alone** and letting it interoperate with the live broker over RabbitMQ.

- `generate_stub.py` parses the spec and generates `qb_stub.py` (channel
  addresses, exchange/queue bindings, content type, delivery modes). It FAILS
  if the spec loses a structure the protocol relies on.
- `test_contract.py` (pytest) plays a mock connector (`PDS-PYSTUB`, mock FHIR
  data) and a mock requester (`responses.pystub`) against the compose stack,
  asserting the wire facts in both directions: fanout delivery, content type,
  `correlation-id`/`reply-to` AMQP properties, persistent delivery, response
  routing per ADR-009, and mock-data round-trip through aggregation.

## Run locally

```bash
cd docker && cp .env.example .env && docker compose up -d --wait broker && cd ..
python3 -m venv .venv-contract && . .venv-contract/bin/activate
pip install -r tools/asyncapi-stub/requirements.txt
pytest tools/asyncapi-stub/test_contract.py -v
```

CI: `.github/workflows/asyncapi-contract-test.yml` runs this on every change
to `specs/**`, the broker, the SDK, or the docker stack.
