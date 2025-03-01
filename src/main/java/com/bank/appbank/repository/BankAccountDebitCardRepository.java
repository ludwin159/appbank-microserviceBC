package com.bank.appbank.repository;

import com.bank.appbank.model.BankAccountDebitCard;
import reactor.core.publisher.Flux;

import java.util.List;

public interface BankAccountDebitCardRepository extends  RepositoryT<BankAccountDebitCard, String> {
    Flux<BankAccountDebitCard> findAllByIdDebitCardIn(List<String> idDebitCards);
    Flux<BankAccountDebitCard> findAllByIdDebitCard(String idDebitCard);
    Flux<BankAccountDebitCard> findAllByIdDebitCardOrderByCreatedAtAsc(String idDebitCard);
}
