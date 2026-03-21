package com.rkortega94.scanner;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ScannerProperties.class)
@EnableConfigurationProperties(ScannerProperties.class)
public class ScannerAutoConfiguration {
}
