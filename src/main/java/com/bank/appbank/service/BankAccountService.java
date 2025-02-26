package com.bank.appbank.service;

import com.bank.appbank.model.BankAccount;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface BankAccountService extends ServiceT<BankAccount, String>{
    Mono<BankAccount> update(String id, BankAccount bankAccount);
    Flux<BankAccount> findBankAccountsByIdClient(String idClient);
    Flux<BankAccount> findBankAccountsByIdClientWithAllMovementsSortedByDate(String idClient);
    Mono<BankAccount> findByIdWithoutMovements(String idBankAccount);
}
