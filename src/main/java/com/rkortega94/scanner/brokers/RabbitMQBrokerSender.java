package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import com.rkortega94.scanner.enums.BrokerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

@Slf4j
public class RabbitMQBrokerSender implements BrokerSenderStrategy {
    private final RabbitTemplate rabbitTemplate;
    private final ScannerProperties properties;


    public RabbitMQBrokerSender(RabbitTemplate rabbitTemplate, ScannerProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void send(ScannedApplicationDTO applicationData) {
        log.error("Sending scanned data to RabbitMQ...");
        ObjectMapper mapper = new ObjectMapper();
        log.error("Data: {}", mapper.writeValueAsString(applicationData));
        rabbitTemplate.convertAndSend(properties.getBroker().getRabbitmq().getExchange(), properties.getBroker().getRabbitmq().getRoutingKey(), applicationData);
    }

    @Override
    public BrokerType getType() {
        return BrokerType.RABBITMQ;
    }
}
