package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import com.rkortega94.scanner.enums.BrokerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMQBrokerSenderTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ScannerProperties properties;
    private RabbitMQBrokerSender rabbitMQBrokerSender;

    @BeforeEach
    void setUp() {
        properties = new ScannerProperties();
        properties.getBroker().getRabbitmq().setExchange("test-exchange");
        properties.getBroker().getRabbitmq().setRoutingKey("test-key");
        rabbitMQBrokerSender = new RabbitMQBrokerSender(rabbitTemplate, properties);
    }

    @Test
    void send_ShouldUseCorrectExchangeAndRoutingKey() {
        ScannedApplicationDTO data = new ScannedApplicationDTO("test-service", new HashSet<>(), new HashSet<>());
        rabbitMQBrokerSender.send(data);
        verify(rabbitTemplate).convertAndSend("test-exchange", "test-key", data);
    }

    @Test
    void getType_ShouldReturnRabbitMQ() {
        assertEquals(BrokerType.RABBITMQ, rabbitMQBrokerSender.getType());
    }
}
