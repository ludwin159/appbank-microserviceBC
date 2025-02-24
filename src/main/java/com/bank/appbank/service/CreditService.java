package com.bank.appbank.service;

import com.bank.appbank.model.Credit;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditService extends ServiceT<Credit, String> {
    Mono<Credit> update(String id, Credit client);
    Flux<Credit> findAllCreditsByIdClient(String idClient);
    Flux<Credit> allCreditsByIdClientWithAllPaymentsSortedByDatePayment(String idClient);
}
