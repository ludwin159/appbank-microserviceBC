package com.bank.appbank.controller;

import com.bank.appbank.service.ServiceT;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

public abstract class ControllerT<T, ID> {
    private final ServiceT<T, ID> serviceT;

    public ControllerT(ServiceT<T, ID> serviceT) {
        this.serviceT = serviceT;
    }

    @GetMapping
    public Flux<T> getAll() {
        return serviceT.getAll();
    }

    @GetMapping("/{id}")
    public Mono<T> findById(@PathVariable ID id) {
        return serviceT.findById(id);
    }

    @PostMapping
    public Mono<T> create(@Valid @RequestBody T document) {
        return serviceT.create(document);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> deleteById(@PathVariable ID id) {
        return serviceT.deleteById(id);
    }
}
