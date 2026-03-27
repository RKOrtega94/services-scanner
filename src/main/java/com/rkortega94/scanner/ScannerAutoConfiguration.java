package com.rkortega94.scanner;

import com.rkortega94.scanner.brokers.KafkaConfiguration;
import com.rkortega94.scanner.brokers.RabbitMQConfiguration;
import com.rkortega94.scanner.brokers.RedisConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(ScannerProperties.class)
@EnableConfigurationProperties(ScannerProperties.class)
@AutoConfigureAfter(name = {
        "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
@Import({
        ScannerService.class,
        ScannerComponent.class,
        KafkaConfiguration.class,
        RabbitMQConfiguration.class,
        RedisConfiguration.class,
})
public class ScannerAutoConfiguration {
}
