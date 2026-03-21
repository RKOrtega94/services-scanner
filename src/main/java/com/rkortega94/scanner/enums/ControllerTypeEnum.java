package com.rkortega94.scanner.enums;

import lombok.Getter;

@Getter
public enum ControllerTypeEnum {
    CONTROLLER("Controller"), REST("REST"), WEB_SOCKET("WebSocket"), GRPC("gRPC"), UNKNOWN("Unknown");

    private final String displayName;

    ControllerTypeEnum(String displayName) {
        this.displayName = displayName;
    }
}