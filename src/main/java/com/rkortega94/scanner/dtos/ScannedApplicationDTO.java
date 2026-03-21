package com.rkortega94.scanner.dtos;

import java.util.Set;

/**
 * Scanned application DTO
 *
 * @param serviceName application service name
 * @param controllers scanned controllers
 */
public record ScannedApplicationDTO(String serviceName, Set<ScannedControllerDTO> controllers) {
}
