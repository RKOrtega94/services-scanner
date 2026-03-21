package com.rkortega94.scanner;

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
}