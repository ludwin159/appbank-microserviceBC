package com.bank.appbank.config;

import com.bank.appbank.model.Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Client> clientRedisTemplate(ReactiveRedisConnectionFactory factory,
                                                                     ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<Client> serializer = new Jackson2JsonRedisSerializer<>(Client.class);
        serializer.setObjectMapper(objectMapper);

        RedisSerializationContext<String, Client> serializationContext =
                RedisSerializationContext.<String, Client>newSerializationContext(new StringRedisSerializer())
                        .key(new StringRedisSerializer())
                        .value(serializer)
                        .hashKey(new StringRedisSerializer())
                        .hashValue(serializer)
                        .build();

        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }
}
