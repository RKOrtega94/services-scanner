package com.rkortega94.scanner;

import com.rkortega94.scanner.brokers.BrokerSenderStrategy;
import com.rkortega94.scanner.dtos.MethodDataDTO;
import com.rkortega94.scanner.dtos.ScannedServiceDTO;
import com.rkortega94.scanner.enums.BrokerType;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ScannerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(ScannerAutoConfiguration.class)).withUserConfiguration(MockDependenciesConfig.class);

    @Test
    void shouldNotHaveBrokerSenderWhenTypeNotSet() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(BrokerSenderStrategy.class);
        });
    }

    @Test
    void shouldHaveKafkaBrokerSenderWhenTypeIsKafka() {
        contextRunner.withPropertyValues("scanner.broker.type=KAFKA").run(context -> {
            assertThat(context).hasSingleBean(BrokerSenderStrategy.class);
            assertThat(context.getBean(BrokerSenderStrategy.class).getType()).isEqualTo(BrokerType.KAFKA);
        });
    }

    @Test
    void shouldHaveRabbitMQBrokerSenderWhenTypeIsRabbitMQ() {
        contextRunner.withPropertyValues("scanner.broker.type=RABBITMQ").run(context -> {
            assertThat(context).hasSingleBean(BrokerSenderStrategy.class);
            assertThat(context.getBean(BrokerSenderStrategy.class).getType()).isEqualTo(BrokerType.RABBITMQ);
        });
    }

    @Test
    void shouldHaveBrokerSenderInScannerComponent() throws Exception {
        contextRunner.withPropertyValues("scanner.broker.type=RABBITMQ").run(context -> {
            ScannerComponent scannerComponent = context.getBean(ScannerComponent.class);
            java.lang.reflect.Field field = ScannerComponent.class.getDeclaredField("brokerSenderStrategyProvider");
            field.setAccessible(true);
            org.springframework.beans.factory.ObjectProvider<BrokerSenderStrategy> provider = (org.springframework.beans.factory.ObjectProvider<BrokerSenderStrategy>) field.get(scannerComponent);
            assertThat(provider.getIfAvailable()).isNotNull();
        });
    }

    @Test
    void shouldHaveRedisBrokerSenderWhenTypeIsRedis() {
        contextRunner.withPropertyValues("scanner.broker.type=REDIS").run(context -> {
            assertThat(context).hasSingleBean(BrokerSenderStrategy.class);
            assertThat(context.getBean(BrokerSenderStrategy.class).getType()).isEqualTo(BrokerType.REDIS);
        });
    }

    @Test
    void shouldScanPrivateMethodsInServices() {
        contextRunner.run(context -> {
            ScannerService scannerService = context.getBean(ScannerService.class);
            var scannedApp = scannerService.scanServices();

            ScannedServiceDTO testService = scannedApp.services().stream().filter(s -> s.serviceName().equals("PrivateMethodTestService")).findFirst().orElseThrow();

            Set<String> methodNames = testService.methods().stream().map(MethodDataDTO::name).collect(java.util.stream.Collectors.toSet());

            assertThat(methodNames).contains("publicMethod", "privateMethod", "protectedMethod", "packagePrivateMethod");
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

        @Bean
        public PrivateMethodTestService privateMethodTestService() {
            return new PrivateMethodTestService();
        }
    }

    @Service
    static class PrivateMethodTestService {
        public void publicMethod() {
        }

        private void privateMethod() {
        }

        protected void protectedMethod() {
        }

        void packagePrivateMethod() {
        }
    }
}
