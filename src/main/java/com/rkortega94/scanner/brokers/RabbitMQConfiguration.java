package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnProperty(prefix = "scanner.broker", name = "type", havingValue = "RABBITMQ")
@ConditionalOnBean(ConnectionFactory.class)
public class RabbitMQConfiguration {

    private final ScannerProperties properties;

    @Bean
    @ConditionalOnMissingBean(name = "scannerRabbitTemplate")
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new JacksonJsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitMQBrokerSender rabbitMQBrokerSender(RabbitTemplate rabbitTemplate) {
        return new RabbitMQBrokerSender(rabbitTemplate, properties);
    }
}
