package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import com.rkortega94.scanner.enums.BrokerType;

public interface BrokerSenderStrategy {
    void send(ScannedApplicationDTO applicationData);

    BrokerType getType();
}
