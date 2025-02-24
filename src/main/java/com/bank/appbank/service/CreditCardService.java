package com.bank.appbank.service;

import com.bank.appbank.model.CreditCard;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditCardService extends ServiceT<CreditCard, String> {
    Mono<CreditCard> update(String id, CreditCard client);
    Flux<CreditCard> allCreditCardsByIdClientWithPaymentAndConsumption(String idClient);
}
