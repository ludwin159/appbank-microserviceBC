package com.bank.appbank.repository;

import com.bank.appbank.model.Credit;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface CreditRepository extends RepositoryT<Credit, String> {
    public Flux<Credit> findAllByIdClient(String idClient);
    public Flux<Credit> findAllByCreatedAtBetween(Instant from, Instant to);

}
