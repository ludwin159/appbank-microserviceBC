package com.bank.appbank.service;

import com.bank.appbank.model.DebitCard;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DebitCardService extends ServiceT<DebitCard, String> {
    public Mono<DebitCard> update(String idDebitCard, DebitCard debitCard);

    Flux<DebitCard> findAllByClientId(String clientId);
}
