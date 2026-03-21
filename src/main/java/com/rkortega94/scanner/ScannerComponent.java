package com.rkortega94.scanner;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "scanner", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScannerComponent {
    private final ScannerProperties properties;
    private final ScannerService scannerService;

    @Scheduled(cron = "${scanner.cron}")
    public void scan() {
        initScanner();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent() {
        initScanner();
    }

    private void initScanner() {
        if (properties.isEnabled()) {
            if (properties.getScanAll()) scannerService.scanAll(properties.getIncludeSwagger());
            else {
                if (properties.getScanControllers()) scannerService.scanControllers();
                if (properties.getScanRestControllers())
                    scannerService.scanRestControllers(properties.getIncludeSwagger());
                if (properties.getScanServices()) scannerService.scanServices();
            }
        }
    }
}