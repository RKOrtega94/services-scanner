package com.rkortega94.scanner;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rkortega94.scanner.brokers.BrokerSenderStrategy;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import com.rkortega94.scanner.enums.BrokerType;

import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
@ConditionalOnProperty(prefix = "scanner", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScannerComponent {
    private final ScannerProperties properties;
    private final ScannerService scannerService;
    private final ObjectProvider<BrokerSenderStrategy> brokerSenderStrategyProvider;

    public ScannerComponent(ScannerProperties properties, ScannerService scannerService,
                            ObjectProvider<BrokerSenderStrategy> brokerSenderStrategyProvider) {
        this.properties = properties;
        this.scannerService = scannerService;
        this.brokerSenderStrategyProvider = brokerSenderStrategyProvider;
        logBrokerConfiguration();
        validateBrokerConfiguration();
    }

    private void logBrokerConfiguration() {
        log.debug("Scanner broker configuration - Type: {}, Enabled: {}", 
            properties.getBroker().getType(), properties.isEnabled());
        if (BrokerType.RABBITMQ.equals(properties.getBroker().getType())) {
            log.debug("RabbitMQ Exchange: {}, Routing Key: {}", 
                properties.getBroker().getRabbitmq().getExchange(),
                properties.getBroker().getRabbitmq().getRoutingKey());
        }
    }

    private void validateBrokerConfiguration() {
        if (!properties.isEnabled()) {
            log.debug("Scanner is disabled (scanner.enabled=false)");
            return;
        }

        BrokerSenderStrategy strategy = brokerSenderStrategyProvider.getIfAvailable();
        if (strategy == null) {
            BrokerType brokerType = properties.getBroker().getType();
            log.warn("No BrokerSenderStrategy bean found for configured broker type: {}. " +
                    "Verify broker configuration and required dependencies.", brokerType);
            
            switch (brokerType) {
                case RABBITMQ:
                    log.warn("RabbitMQ configuration validation: " +
                            "Ensure 'spring.rabbitmq.host' is set and 'spring-boot-starter-amqp' dependency is present. " +
                            "Configured exchange: {}, routing-key: {}",
                            properties.getBroker().getRabbitmq().getExchange(),
                            properties.getBroker().getRabbitmq().getRoutingKey());
                    break;
                case KAFKA:
                    log.warn("Kafka configuration validation: " +
                            "Ensure 'spring.kafka.bootstrap-servers' is set and 'spring-kafka' dependency is present.");
                    break;
                case REDIS:
                    log.warn("Redis configuration validation: " +
                            "Ensure 'spring.redis.host' is set and 'spring-boot-starter-data-redis' dependency is present. " +
                            "Configured channel: {}",
                            properties.getBroker().getRedis().getChannel());
                    break;
                default:
                    log.warn("Unknown broker type: {}", brokerType);
            }
        }
    }

    @Scheduled(cron = "${scanner.cron:0 0 0 * * *}")
    public void scan() {
        log.info("Scanner execution triggered by cron.");
        initScanner();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent() {
        if (Boolean.TRUE.equals(properties.getScanOnStartup())) {
            log.info("Scanner execution triggered by ApplicationReadyEvent.");
            initScanner();
        } else {
            log.debug("Scanner skip on startup: property 'scanner.scan-on-startup' is false.");
        }
    }

    private void initScanner() {
        if (properties.isEnabled()) {
            log.debug("Initializing scanner scanning process...");
            if (Boolean.TRUE.equals(properties.getScanAll())) {
                sendData(scannerService.scanAll(properties.getIncludeSwagger()));
            } else {
                if (Boolean.TRUE.equals(properties.getScanControllers())) {
                    sendData(scannerService.scanControllers());
                }
                if (Boolean.TRUE.equals(properties.getScanRestControllers())) {
                    sendData(scannerService.scanRestControllers(properties.getIncludeSwagger()));
                }
                if (Boolean.TRUE.equals(properties.getScanServices())) {
                    sendData(scannerService.scanServices());
                }
            }
        }
    }

    private void sendData(ScannedApplicationDTO data) {
        BrokerSenderStrategy strategy = brokerSenderStrategyProvider.getIfAvailable();
        if (strategy != null) {
            log.info("Sending scanned data using broker: {}", strategy.getType());
            strategy.send(data);
        } else {
            log.error("Failed to send scanned data: No BrokerSenderStrategy bean available. " +
                    "Configured broker type: {}. This typically indicates a configuration or dependency issue. " +
                    "Check application logs for 'No BrokerSenderStrategy bean found' warnings during startup.",
                    properties.getBroker().getType());
        }
    }
}