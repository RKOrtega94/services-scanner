package com.rkortega94.scanner.dtos;

import java.util.Set;

/**
 * Scanned service DTO
 *
 * @param serviceName service class name
 * @param methods     service methods
 */
public record ScannedServiceDTO(String serviceName, Set<MethodDataDTO> methods) {
}
