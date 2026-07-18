package de.tudresden.fgdh.querybroker.pdsexample;

import ca.uhn.fhir.context.FhirContext;
import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares this connector's request queue and its bindings (idempotent against
 * docker/rabbitmq/definitions.json). During the fanout→topic migration
 * (ADR-006) the queue is DUAL-bound: to the fanout exchange (legacy broadcast)
 * AND to the topic exchange with this site's routing key
 * {@code pds.{pdsId}.request}, so it receives requests in either broker mode.
 */
@Configuration
public class ConnectorAmqpConfig {

  @Bean
  public FhirContext fhirContext() {
    return FhirContext.forR4();
  }

  @Bean
  public FanoutExchange broadcastExchange() {
    return new FanoutExchange(BrokerProtocol.BROADCAST_EXCHANGE, true, false);
  }

  @Bean
  public TopicExchange topicExchange() {
    return new TopicExchange(BrokerProtocol.TOPIC_EXCHANGE, true, false);
  }

  @Bean
  public Queue requestQueue(
      @Value("${pds.connector.request-queue:req.PDS-EXAMPLE}") String requestQueue) {
    return QueueBuilder.durable(requestQueue).deadLetterExchange("pds.dlq").build();
  }

  @Bean
  public Binding broadcastBinding(Queue requestQueue, FanoutExchange broadcastExchange) {
    return BindingBuilder.bind(requestQueue).to(broadcastExchange);
  }

  @Bean
  public Binding topicBinding(
      Queue requestQueue,
      TopicExchange topicExchange,
      @Value("${pds.connector.pds-id:PDS-EXAMPLE}") String pdsId) {
    return BindingBuilder.bind(requestQueue)
        .to(topicExchange)
        .with(BrokerProtocol.requestRoutingKey(pdsId));
  }
}
