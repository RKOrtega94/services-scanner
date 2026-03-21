package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import com.rkortega94.scanner.enums.BrokerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaBrokerSenderTest {

    @Mock
    private KafkaTemplate<String, ScannedApplicationDTO> kafkaTemplate;

    private ScannerProperties properties;
    private KafkaBrokerSender kafkaBrokerSender;

    @BeforeEach
    void setUp() {
        properties = new ScannerProperties();
        properties.getBroker().getKafka().setTopic("test-topic");
        kafkaBrokerSender = new KafkaBrokerSender(kafkaTemplate, properties);
    }

    @Test
    void send_ShouldUseCorrectTopic() {
        ScannedApplicationDTO data = new ScannedApplicationDTO("test-service", new HashSet<>(), new HashSet<>());
        kafkaBrokerSender.send(data);
        verify(kafkaTemplate).send("test-topic", data);
    }

    @Test
    void getType_ShouldReturnKafka() {
        assertEquals(BrokerType.KAFKA, kafkaBrokerSender.getType());
    }
}
