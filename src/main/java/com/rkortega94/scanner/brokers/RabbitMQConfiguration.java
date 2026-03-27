package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(prefix = "scanner.broker", name = "type", havingValue = "RABBITMQ")
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration")
public class RabbitMQConfiguration {

    private static final String DEAD_LETTER_QUEUE = "scanner.dlq";
    private static final String DEAD_LETTER_EXCHANGE = "scanner.dlx";

    private final ScannerProperties properties;

    @Bean(name = "scannerExchange")
    public Exchange scannerExchange() {
        return new TopicExchange(properties.getBroker().getRabbitmq().getExchange(), true, false);
    }

    @Bean(name = "scannerQueue")
    public Queue scannerQueue() {
        return QueueBuilder.durable(properties.getBroker().getRabbitmq().getExchange() + ".queue").withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE).withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE).build();
    }

    @Bean(name = "scannerBinding")
    public Binding scannerBinding(@Qualifier("scannerQueue") Queue scannerQueue, @Qualifier("scannerExchange") Exchange scannerExchange) {
        return BindingBuilder.bind(scannerQueue).to((TopicExchange) scannerExchange).with(properties.getBroker().getRabbitmq().getRoutingKey());
    }

    @Bean(name = "scannerRabbitTemplate")
    @ConditionalOnMissingBean(name = "scannerRabbitTemplate")
    public RabbitTemplate scannerRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new JacksonJsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public RabbitAdmin scannerRabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setIgnoreDeclarationExceptions(true);
        return admin;
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitMQBrokerSender rabbitMQBrokerSender(@Qualifier("scannerRabbitTemplate") RabbitTemplate scannerRabbitTemplate) {
        return new RabbitMQBrokerSender(scannerRabbitTemplate, properties);
    }

    @Bean(name = "deadLetterQueue")
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean(name = "deadLetterExchange")
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE);
    }

    @Bean(name = "deadLetterBinding")
    public Binding deadLetterBinding(@Qualifier("deadLetterQueue") Queue deadLetterQueue, @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_QUEUE);
    }
}