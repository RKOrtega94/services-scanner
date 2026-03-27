package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisBrokerSenderIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
            .withExposedPorts(6379);

    private static ScannerProperties properties;
    private static RedisConfiguration redisConfiguration;

    @BeforeAll
    static void setUp() {
        properties = new ScannerProperties();
        properties.getBroker().getRedis().setChannel("test-channel");

        redisConfiguration = new RedisConfiguration(properties);
    }

    @Test
    void shouldSendToRedis() throws InterruptedException {
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379)));
        connectionFactory.afterPropertiesSet();

        RedisTemplate<String, ScannedApplicationDTO> redisTemplate = redisConfiguration.scannerRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        RedisBrokerSender sender = new RedisBrokerSender(redisTemplate, properties);
        ScannedApplicationDTO data = new ScannedApplicationDTO("test-service", new HashSet<>());

        // Use a listener to catch the message
        BlockingQueue<ScannedApplicationDTO> receivedMessages = new LinkedBlockingQueue<>();
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        MessageListenerAdapter adapter = new MessageListenerAdapter(new Object() {
            @SuppressWarnings("unused")
            public void handleMessage(ScannedApplicationDTO message) {
                receivedMessages.add(message);
            }
        }, "handleMessage");
        adapter.setSerializer(redisTemplate.getValueSerializer());
        adapter.afterPropertiesSet();

        container.addMessageListener(adapter, new ChannelTopic("test-channel"));
        container.afterPropertiesSet();
        container.start();

        // Small delay to ensure container is subscribed
        Thread.sleep(1000);

        sender.send(data);

        ScannedApplicationDTO received = receivedMessages.poll(10, TimeUnit.SECONDS);

        assertThat(received).isNotNull();
        assertThat(received.serviceName()).isEqualTo("test-service");

        container.stop();
        connectionFactory.destroy();
    }
}
