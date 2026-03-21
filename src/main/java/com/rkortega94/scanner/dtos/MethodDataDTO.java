package com.rkortega94.scanner.dtos;

import lombok.Builder;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Set;

@Builder
public record MethodDataDTO(String name, Set<RequestMethod> methods, Set<String> paths, Set<String> roles,
                            Set<String> authorities) {
}
