package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import com.rkortega94.scanner.enums.BrokerType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RabbitMQBrokerSender implements BrokerSenderStrategy {
    private final RabbitTemplate rabbitTemplate;
    private final ScannerProperties properties;

    public RabbitMQBrokerSender(RabbitTemplate rabbitTemplate, ScannerProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void send(ScannedApplicationDTO applicationData) {
        rabbitTemplate.convertAndSend(
                properties.getBroker().getRabbitmq().getExchange(),
                properties.getBroker().getRabbitmq().getRoutingKey(),
                applicationData
        );
    }

    @Override
    public BrokerType getType() {
        return BrokerType.RABBITMQ;
    }
}
