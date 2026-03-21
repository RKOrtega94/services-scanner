package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import com.rkortega94.scanner.enums.BrokerType;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisBrokerSender implements BrokerSenderStrategy {
    private final RedisTemplate<String, ScannedApplicationDTO> redisTemplate;
    private final ScannerProperties properties;

    public RedisBrokerSender(RedisTemplate<String, ScannedApplicationDTO> redisTemplate, ScannerProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public void send(ScannedApplicationDTO applicationData) {
        redisTemplate.convertAndSend(properties.getBroker().getRedis().getChannel(), applicationData);
    }

    @Override
    public BrokerType getType() {
        return BrokerType.REDIS;
    }
}
