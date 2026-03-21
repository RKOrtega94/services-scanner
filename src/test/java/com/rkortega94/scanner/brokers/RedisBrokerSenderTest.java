package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import com.rkortega94.scanner.enums.BrokerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisBrokerSenderTest {

    @Mock
    private RedisTemplate<String, ScannedApplicationDTO> redisTemplate;

    private ScannerProperties properties;
    private RedisBrokerSender redisBrokerSender;

    @BeforeEach
    void setUp() {
        properties = new ScannerProperties();
        properties.getBroker().getRedis().setChannel("test-channel");
        redisBrokerSender = new RedisBrokerSender(redisTemplate, properties);
    }

    @Test
    void send_ShouldUseCorrectChannel() {
        ScannedApplicationDTO data = new ScannedApplicationDTO("test-service", new HashSet<>(), new HashSet<>());
        redisBrokerSender.send(data);
        verify(redisTemplate).convertAndSend("test-channel", data);
    }

    @Test
    void getType_ShouldReturnRedis() {
        assertEquals(BrokerType.REDIS, redisBrokerSender.getType());
    }
}
