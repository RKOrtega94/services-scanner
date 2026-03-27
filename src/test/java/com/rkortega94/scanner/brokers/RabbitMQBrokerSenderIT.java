package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RabbitMQBrokerSenderIT {

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.12-management"));

    private static ScannerProperties properties;
    private static RabbitMQConfiguration rabbitMQConfiguration;

    @BeforeAll
    static void setUp() {
        properties = new ScannerProperties();
        properties.getBroker().getRabbitmq().setExchange("test-exchange");
        properties.getBroker().getRabbitmq().setRoutingKey("test-routing-key");

        rabbitMQConfiguration = new RabbitMQConfiguration(properties);
    }

    @Test
    void shouldSendToRabbitMQ() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitmq.getHost(), rabbitmq.getAmqpPort());
        RabbitTemplate rabbitTemplate = rabbitMQConfiguration.scannerRabbitTemplate(connectionFactory);
        
        RabbitMQBrokerSender sender = new RabbitMQBrokerSender(rabbitTemplate, properties);
        ScannedApplicationDTO data = new ScannedApplicationDTO("test-service", new HashSet<>());

        // We need to ensure the exchange exists
        rabbitTemplate.execute(channel -> {
            channel.exchangeDeclare("test-exchange", "topic", true);
            channel.queueDeclare("test-queue", true, false, false, null);
            channel.queueBind("test-queue", "test-exchange", "test-routing-key");
            return null;
        });

        sender.send(data);

        // Receive and verify
        rabbitTemplate.setMessageConverter(new JacksonJsonMessageConverter());
        ScannedApplicationDTO received = (ScannedApplicationDTO) rabbitTemplate.receiveAndConvert("test-queue", 10000);

        assertThat(received).isNotNull();
        assertThat(received.serviceName()).isEqualTo("test-service");
        
        connectionFactory.destroy();
    }
}
