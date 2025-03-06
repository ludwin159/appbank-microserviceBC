package com.bank.appbank.service.impl;

import com.bank.appbank.model.Client;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class ClientRedisService {
    private final ReactiveRedisTemplate<String, Client> redisTemplate;
    private static final String redisPrefix = "client:";

    public ClientRedisService(ReactiveRedisTemplate<String, Client> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Void> save(String key, Client value, Duration duration) {
        return redisTemplate.opsForValue().set(redisPrefix + key, value, duration).then();
    }

    public Mono<Client> get(String key) {
        return redisTemplate.opsForValue().get(redisPrefix + key);
    }

    public Mono<Boolean> delete(String key) {
        return redisTemplate.opsForValue().delete(redisPrefix + key);
    }
}
