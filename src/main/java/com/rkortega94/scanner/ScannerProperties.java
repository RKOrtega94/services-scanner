package com.rkortega94.scanner;

import com.rkortega94.scanner.enums.BrokerType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "scanner")
public class ScannerProperties {
    private boolean enabled = false;
    private String cron = "0 0 0 * * *";
    private Boolean scanOnStartup = false;
    private Boolean scanAll = false;
    private Boolean scanControllers = false;
    private Boolean scanRestControllers = false;
    private Boolean scanServices = false;
    private Boolean includeSwagger = false;
    private BrokerProperties broker = new BrokerProperties();

    @Getter
    @Setter
    public static class BrokerProperties {
        private BrokerType type;
        private KafkaBrokerProperties kafka = new KafkaBrokerProperties();
        private RabbitBrokerProperties rabbitmq = new RabbitBrokerProperties();
        private RedisBrokerProperties redis = new RedisBrokerProperties();

        @Getter
        @Setter
        public static class KafkaBrokerProperties {
            private String topic;
        }

        @Getter
        @Setter
        public static class RabbitBrokerProperties {
            private String exchange;
            private String routingKey;
        }

        @Getter
        @Setter
        public static class RedisBrokerProperties {
            private String channel;
        }
    }
}