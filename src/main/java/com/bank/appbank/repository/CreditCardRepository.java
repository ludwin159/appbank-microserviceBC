package com.bank.appbank.repository;


import com.bank.appbank.model.CreditCard;
import reactor.core.publisher.Flux;

public interface CreditCardRepository extends RepositoryT<CreditCard, String> {
    Flux<CreditCard> findAllByIdClient(String idClient);
}
