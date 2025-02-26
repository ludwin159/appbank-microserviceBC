package com.bank.appbank.repository;


import com.bank.appbank.model.Credit;
import com.bank.appbank.model.CreditCard;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface CreditCardRepository extends RepositoryT<CreditCard, String> {
    Flux<CreditCard> findAllByIdClient(String idClient);
    public Flux<CreditCard> findAllByCreatedAtBetween(Instant from, Instant to);
}
