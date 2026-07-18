package de.tudresden.fgdh.querybroker.broker;

import de.tudresden.fgdh.querybroker.sdk.BrokerProtocol;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the broker-side AMQP topology (idempotent against the definitions
 * preloaded by docker/rabbitmq/definitions.json). Both exchanges are declared
 * so either routing mode works during the fanout→topic migration (ADR-006).
 */
@Configuration
public class AmqpConfig {

  @Bean
  public FanoutExchange broadcastExchange() {
    return new FanoutExchange(BrokerProtocol.BROADCAST_EXCHANGE, true, false);
  }

  @Bean
  public TopicExchange topicExchange() {
    return new TopicExchange(BrokerProtocol.TOPIC_EXCHANGE, true, false);
  }

  @Bean
  public Queue brokerReplyQueue(BrokerProperties properties) {
    return QueueBuilder.durable(properties.replyQueue()).build();
  }

  @Bean
  public Queue defaultResponseQueue() {
    return QueueBuilder.durable(BrokerProtocol.DEFAULT_RESPONSE_QUEUE).build();
  }
}
