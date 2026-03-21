package com.rkortega94.scanner.dtos;

import java.util.Set;

import com.rkortega94.scanner.enums.ControllerTypeEnum;

/**
 * Scanned controller DTO
 *
 * @param controllerName controller name
 * @param paths          controller paths
 * @param methods        controller methods
 * @param controllerType controller type
 */
public record ScannedControllerDTO(String controllerName, Set<String> paths, Set<MethodDataDTO> methods,
                                   ControllerTypeEnum controllerType) {
}
