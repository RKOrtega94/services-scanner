package com.rkortega94.scanner;

import com.rkortega94.scanner.brokers.BrokerSenderStrategy;
import com.rkortega94.scanner.enums.BrokerType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ScannerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ScannerAutoConfiguration.class))
            .withUserConfiguration(MockDependenciesConfig.class);

    @Test
    void shouldNotHaveBrokerSenderWhenTypeNotSet() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(BrokerSenderStrategy.class);
        });
    }

    @Test
    void shouldHaveKafkaBrokerSenderWhenTypeIsKafka() {
        contextRunner.withPropertyValues("scanner.broker.type=KAFKA")
                .run(context -> {
                    assertThat(context).hasSingleBean(BrokerSenderStrategy.class);
                    assertThat(context.getBean(BrokerSenderStrategy.class).getType()).isEqualTo(BrokerType.KAFKA);
                });
    }

    @Test
    void shouldHaveRabbitMQBrokerSenderWhenTypeIsRabbitMQ() {
        contextRunner.withPropertyValues("scanner.broker.type=RABBITMQ")
                .run(context -> {
                    assertThat(context).hasSingleBean(BrokerSenderStrategy.class);
                    assertThat(context.getBean(BrokerSenderStrategy.class).getType()).isEqualTo(BrokerType.RABBITMQ);
                });
    }

    @Test
    void shouldHaveRedisBrokerSenderWhenTypeIsRedis() {
        contextRunner.withPropertyValues("scanner.broker.type=REDIS")
                .run(context -> {
                    assertThat(context).hasSingleBean(BrokerSenderStrategy.class);
                    assertThat(context.getBean(BrokerSenderStrategy.class).getType()).isEqualTo(BrokerType.REDIS);
                });
    }

    @Configuration
    static class MockDependenciesConfig {

        @Bean
        public ConnectionFactory connectionFactory() {
            return mock(ConnectionFactory.class);
        }

        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }
    }
}
