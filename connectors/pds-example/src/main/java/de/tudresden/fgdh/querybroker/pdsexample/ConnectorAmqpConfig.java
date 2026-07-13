package de.tudresden.fgdh.querybroker.pdsexample;

import ca.uhn.fhir.context.FhirContext;
import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares this connector's request queue and its binding to the broadcast
 * exchange (idempotent against docker/rabbitmq/definitions.json; mirrors the
 * rabbitmqadmin steps in CONTRIBUTING § 2, step 5).
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
  public Queue requestQueue(
      @Value("${pds.connector.request-queue:req.PDS-EXAMPLE}") String requestQueue) {
    return QueueBuilder.durable(requestQueue).deadLetterExchange("pds.dlq").build();
  }

  @Bean
  public Binding broadcastBinding(Queue requestQueue, FanoutExchange broadcastExchange) {
    return BindingBuilder.bind(requestQueue).to(broadcastExchange);
  }
}
