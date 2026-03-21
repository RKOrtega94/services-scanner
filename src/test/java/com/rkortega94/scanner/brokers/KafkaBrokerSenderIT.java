package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class KafkaBrokerSenderIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    private static KafkaConfiguration kafkaConfiguration;
    private static KafkaBrokerSender kafkaBrokerSender;
    private static ScannerProperties properties;

    @BeforeAll
    static void setUp() {
        properties = new ScannerProperties();
        properties.getBroker().getKafka().setTopic("test-topic");

        kafkaConfiguration = new KafkaConfiguration(properties);
        // Inject bootstrap servers into properties or system properties if needed
        System.setProperty("spring.kafka.bootstrap-servers", kafka.getBootstrapServers());
        
        // We need to manually recreate the bean because @Value is used in KafkaConfiguration
        // and we are not using a full Spring Context here for speed, but we could.
        // For a true IT, let's use a minimal Spring context if possible, 
        // but manually constructing for now to verify the logic.
    }

    @Test
    void shouldSendToKafka() {
        KafkaBrokerSender sender = new KafkaBrokerSender(kafkaConfiguration.kafkaTemplate(), properties);
        ScannedApplicationDTO data = new ScannedApplicationDTO("test-service", new HashSet<>());

        sender.send(data);

        // Verify with a consumer
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        consumerProps.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");

        try (KafkaConsumer<String, ScannedApplicationDTO> consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), new JacksonJsonDeserializer<>(ScannedApplicationDTO.class, false))) {
            consumer.subscribe(Collections.singletonList("test-topic"));
            ConsumerRecord<String, ScannedApplicationDTO> record = consumer.poll(Duration.ofSeconds(10)).iterator().next();
            
            assertThat(record.value().serviceName()).isEqualTo("test-service");
        }
    }
}
