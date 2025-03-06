package com.bank.appbank.repository;

import com.bank.appbank.model.DebitCard;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DebitCardRepository extends RepositoryT<DebitCard, String> {
    Flux<DebitCard> findAllByIdClient(String idClient);
    Flux<DebitCard> findAllByIdPrincipalAccount(String idPrincipalBankAccount);
    Flux<DebitCard> findAllByIdIn(List<String> idsDebitCard);
}
