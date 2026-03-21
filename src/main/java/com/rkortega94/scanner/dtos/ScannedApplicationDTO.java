package com.rkortega94.scanner.dtos;

import java.util.Set;

/**
 * Scanned application DTO
 *
 * @param serviceName application service name
 * @param controllers scanned controllers
 * @param services    scanned services
 */
public record ScannedApplicationDTO(String serviceName, Set<ScannedControllerDTO> controllers,
                                   Set<ScannedServiceDTO> services) {
    public ScannedApplicationDTO(String serviceName, Set<ScannedControllerDTO> controllers) {
        this(serviceName, controllers, Set.of());
    }
}
