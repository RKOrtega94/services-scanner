package com.rkortega94.scanner.brokers;

import com.rkortega94.scanner.ScannerProperties;
import com.rkortega94.scanner.dtos.ScannedApplicationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnProperty(prefix = "scanner.broker", name = "type", havingValue = "REDIS")
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisConfiguration {

    private final ScannerProperties properties;

    @Bean
    @ConditionalOnMissingBean(name = "scannerRedisTemplate")
    public RedisTemplate<String, ScannedApplicationDTO> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ScannedApplicationDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(ScannedApplicationDTO.class));
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisBrokerSender redisBrokerSender(RedisTemplate<String, ScannedApplicationDTO> redisTemplate) {
        return new RedisBrokerSender(redisTemplate, properties);
    }
}
