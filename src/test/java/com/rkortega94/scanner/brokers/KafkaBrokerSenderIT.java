package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
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
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class KafkaBrokerSenderIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    private static KafkaConfiguration kafkaConfiguration;
    private static ScannerProperties properties;

    @BeforeAll
    static void setUp() throws Exception {
        properties = new ScannerProperties();
        properties.getBroker().getKafka().setTopic("test-topic");

        kafkaConfiguration = new KafkaConfiguration(properties);

        // Use reflection to set private field bootstrapServers since we're not using Spring Context
        java.lang.reflect.Field field = KafkaConfiguration.class.getDeclaredField("bootstrapServers");
        field.setAccessible(true);
        field.set(kafkaConfiguration, kafka.getBootstrapServers());
    }

    @Test
    void shouldSendToKafka() {
        KafkaBrokerSender sender = new KafkaBrokerSender(kafkaConfiguration.scannerKafkaTemplate(), properties);
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
