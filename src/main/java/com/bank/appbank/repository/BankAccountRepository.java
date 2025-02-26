package com.bank.appbank.repository;

import com.bank.appbank.model.BankAccount;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface BankAccountRepository extends RepositoryT<BankAccount, String> {
    Flux<BankAccount> findAllByIdClient(String idClient);
    Flux<BankAccount> findAllByCreatedAtBetween(Instant from, Instant to);
}
