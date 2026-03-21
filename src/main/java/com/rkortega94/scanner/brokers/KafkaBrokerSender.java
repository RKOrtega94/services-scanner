package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import com.rkortega94.scanner.enums.BrokerType;
import org.springframework.kafka.core.KafkaTemplate;

public class KafkaBrokerSender implements BrokerSenderStrategy {
    private final KafkaTemplate<String, ScannedApplicationDTO> kafkaTemplate;
    private final ScannerProperties properties;

    public KafkaBrokerSender(KafkaTemplate<String, ScannedApplicationDTO> kafkaTemplate, ScannerProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void send(ScannedApplicationDTO applicationData) {
        kafkaTemplate.send(properties.getBroker().getKafka().getTopic(), applicationData);
    }

    @Override
    public BrokerType getType() {
        return BrokerType.KAFKA;
    }
}
