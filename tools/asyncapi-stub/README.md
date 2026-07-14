# AsyncAPI contract test (official-toolchain stubs)

Proves that `specs/pds-connector-base.yaml` is the contract the running system
actually speaks — by generating **Python and Java stubs with the official
[AsyncAPI Generator](https://www.asyncapi.com/tools/generator)** (via
`@asyncapi/cli`) and testing them against the live broker.

## Why an in-repo template

The official language templates do **not** support AsyncAPI v3 documents yet —
empirically verified: [`@asyncapi/java-spring-template`
(#308)](https://github.com/asyncapi/java-spring-template/issues/308) and
[`@asyncapi/python-paho-template`
(#189)](https://github.com/asyncapi/python-paho-template/issues/189) (paho is
MQTT-only besides). The compatibility-correct path within the recommended
toolchain is the Generator's own extension mechanism: the local react-engine
template `tools/asyncapi-templates/qb-transport-stub` (parser API v3), which
emits:

- `generated/qb_stub.py` — consumed by `test_contract.py` (a mock Python PDS
  connector interoperating with the live broker, mock FHIR data);
- `generated/BrokerTransportSpec.java` — compared constant-by-constant against
  the connector SDK's `BrokerProtocol` by `JavaStubCheck.java`.

Generation fails hard if the spec loses a structure the protocol relies on.

## Run locally

```bash
# one-time: template dependencies
(cd tools/asyncapi-templates/qb-transport-stub && npm ci)

# generate both stubs with the official CLI (pinned)
npx --yes @asyncapi/cli@4.1.1 generate fromTemplate specs/pds-connector-base.yaml \
  ./tools/asyncapi-templates/qb-transport-stub -o tools/asyncapi-stub/generated --force-write

# Java spec/implementation drift check
javac -d /tmp/qb-stubcheck \
  connector-sdk/src/main/java/de/tudresden/fgdh/querybroker/sdk/BrokerProtocol.java \
  tools/asyncapi-stub/generated/BrokerTransportSpec.java tools/asyncapi-stub/JavaStubCheck.java
java -cp /tmp/qb-stubcheck JavaStubCheck

# Python contract test against the compose stack
cd docker && cp .env.example .env && docker compose up -d --wait broker && cd ..
python3 -m venv .venv-contract && . .venv-contract/bin/activate
pip install -r tools/asyncapi-stub/requirements.txt
pytest tools/asyncapi-stub/test_contract.py -v
```

CI: `.github/workflows/asyncapi-contract-test.yml` runs generation, the Java
check, and the Python contract test on every change to `specs/**`, the broker,
the SDK, the docker stack, or this tooling.
