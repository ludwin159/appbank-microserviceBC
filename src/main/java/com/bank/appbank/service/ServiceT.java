package com.bank.appbank.service;

import com.bank.appbank.model.CreditCard;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ServiceT<T, ID> {
    public Flux<T> getAll();
    public Mono<T> findById(ID id);
    public Mono<T> create(T document);
    public Mono<Void> deleteById(ID id);
}
