'use strict';

require('source-map-support/register');
var generatorReactSdk = require('@asyncapi/generator-react-sdk');
var jsxRuntime = require('/Users/marcel/.npm/_npx/99fd4d9a0ad91ec3/node_modules/@asyncapi/generator/node_modules/react/cjs/react-jsx-runtime.production.min.js');

function extractFacts(asyncapi) {
  const fail = msg => {
    throw new Error(`qb-transport-stub: SPEC CONTRACT BROKEN — ${msg}`);
  };
  const contentType = asyncapi.defaultContentType();
  if (!contentType) fail('defaultContentType missing');
  const amqpBindingOf = bindable => {
    const binding = bindable.bindings().all().find(b => b.protocol() === 'amqp');
    return binding ? binding.json() : null;
  };
  let broadcast = null;
  let requestQueue = null;
  let responsesQueue = null;
  let dlq = null;
  for (const channel of asyncapi.channels().all()) {
    const address = channel.address() || '';
    const amqp = amqpBindingOf(channel);
    if (!amqp) continue;
    if (amqp.is === 'routingKey' && amqp.exchange) {
      broadcast = {
        address,
        ...amqp.exchange
      };
    } else if (amqp.is === 'queue') {
      const queue = {
        address,
        ...(amqp.queue || {})
      };
      if (address.startsWith('req.')) requestQueue = queue;else if (address.startsWith('responses.')) responsesQueue = queue;else if (address === 'pds.dlq') dlq = queue;
    }
  }
  if (!broadcast) fail('no exchange-bound (broadcast) channel found');
  if (broadcast.type !== 'fanout') fail(`broadcast exchange type ${broadcast.type}, expected fanout`);
  if (!requestQueue) fail('no req.{pdsId} queue channel found');
  if (!responsesQueue) fail('no responses.{systemId} queue channel found');
  if (!dlq) fail('no pds.dlq channel found');
  const deliveryModes = {};
  for (const required of ['publishRequest', 'consumeRequest', 'publishConnectorResponse', 'publishAggregatedResponse']) {
    const operation = asyncapi.operations().get(required);
    if (!operation) fail(`operation ${required} missing`);
    const amqp = amqpBindingOf(operation);
    deliveryModes[required] = amqp && amqp.deliveryMode || 1;
  }
  return {
    title: asyncapi.info().title(),
    version: asyncapi.info().version(),
    contentType,
    broadcast,
    requestQueue,
    responsesQueue,
    dlq,
    deliveryModes
  };
}
function pythonStub(f) {
  return `"""GENERATED from the AsyncAPI spec by qb-transport-stub via @asyncapi/cli — do not edit."""

SPEC_TITLE = ${JSON.stringify(f.title)}
SPEC_VERSION = ${JSON.stringify(f.version)}
CONTENT_TYPE = ${JSON.stringify(f.contentType)}

BROADCAST_EXCHANGE = ${JSON.stringify(f.broadcast.name)}
BROADCAST_EXCHANGE_TYPE = ${JSON.stringify(f.broadcast.type)}
BROADCAST_EXCHANGE_DURABLE = ${f.broadcast.durable ? 'True' : 'False'}

REQUEST_QUEUE_TEMPLATE = ${JSON.stringify(f.requestQueue.address)}
REQUEST_QUEUE_DURABLE = ${f.requestQueue.durable ? 'True' : 'False'}
RESPONSE_QUEUE_TEMPLATE = ${JSON.stringify(f.responsesQueue.address)}
RESPONSE_QUEUE_DURABLE = ${f.responsesQueue.durable ? 'True' : 'False'}
DEAD_LETTER_QUEUE = ${JSON.stringify(f.dlq.address)}

DELIVERY_MODE_REQUEST = ${f.deliveryModes.publishRequest}
DELIVERY_MODE_CONNECTOR_RESPONSE = ${f.deliveryModes.publishConnectorResponse}
DELIVERY_MODE_AGGREGATED_RESPONSE = ${f.deliveryModes.publishAggregatedResponse}


def request_queue(pds_id: str) -> str:
    return REQUEST_QUEUE_TEMPLATE.replace("{pdsId}", pds_id)


def response_queue(system_id: str) -> str:
    return RESPONSE_QUEUE_TEMPLATE.replace("{systemId}", system_id)
`;
}
function javaStub(f) {
  return `package de.tudresden.fgdh.querybroker.spec;

/** GENERATED from the AsyncAPI spec by qb-transport-stub via @asyncapi/cli — do not edit. */
public final class BrokerTransportSpec {

  public static final String SPEC_TITLE = ${JSON.stringify(f.title)};
  public static final String SPEC_VERSION = ${JSON.stringify(f.version)};
  public static final String CONTENT_TYPE = ${JSON.stringify(f.contentType)};

  public static final String BROADCAST_EXCHANGE = ${JSON.stringify(f.broadcast.name)};
  public static final String BROADCAST_EXCHANGE_TYPE = ${JSON.stringify(f.broadcast.type)};
  public static final boolean BROADCAST_EXCHANGE_DURABLE = ${!!f.broadcast.durable};

  public static final String REQUEST_QUEUE_TEMPLATE = ${JSON.stringify(f.requestQueue.address)};
  public static final boolean REQUEST_QUEUE_DURABLE = ${!!f.requestQueue.durable};
  public static final String RESPONSE_QUEUE_TEMPLATE = ${JSON.stringify(f.responsesQueue.address)};
  public static final boolean RESPONSE_QUEUE_DURABLE = ${!!f.responsesQueue.durable};
  public static final String DEAD_LETTER_QUEUE = ${JSON.stringify(f.dlq.address)};

  public static final int DELIVERY_MODE_REQUEST = ${f.deliveryModes.publishRequest};
  public static final int DELIVERY_MODE_CONNECTOR_RESPONSE = ${f.deliveryModes.publishConnectorResponse};
  public static final int DELIVERY_MODE_AGGREGATED_RESPONSE = ${f.deliveryModes.publishAggregatedResponse};

  public static String requestQueue(String pdsId) {
    return REQUEST_QUEUE_TEMPLATE.replace("{pdsId}", pdsId);
  }

  public static String responseQueue(String systemId) {
    return RESPONSE_QUEUE_TEMPLATE.replace("{systemId}", systemId);
  }

  private BrokerTransportSpec() {}
}
`;
}
function index ({
  asyncapi
}) {
  const facts = extractFacts(asyncapi);
  return [/*#__PURE__*/jsxRuntime.jsx(generatorReactSdk.File, {
    name: "qb_stub.py",
    children: pythonStub(facts)
  }), /*#__PURE__*/jsxRuntime.jsx(generatorReactSdk.File, {
    name: "BrokerTransportSpec.java",
    children: javaStub(facts)
  })];
}

module.exports = index;
//# sourceMappingURL=index.js.map
