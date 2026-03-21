package com.rkortega94.scanner;

import com.rkortega94.scanner.brokers.BrokerSenderStrategy;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "scanner", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScannerComponent {
    private final ScannerProperties properties;
    private final ScannerService scannerService;
    private final Optional<BrokerSenderStrategy> brokerSenderStrategy;

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
        if (brokerSenderStrategy.isPresent()) {
            BrokerSenderStrategy strategy = brokerSenderStrategy.get();
            log.info("Sending scanned data using broker: {}", strategy.getType());
            strategy.send(data);
        } else {
            log.warn("No BrokerSenderStrategy found. Scanned data will not be sent.");
        }
    }
}