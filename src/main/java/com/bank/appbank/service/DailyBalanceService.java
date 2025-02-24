package com.bank.appbank.service;

import com.bank.appbank.model.BankAccount;
import com.bank.appbank.model.Credit;
import com.bank.appbank.model.CreditCard;
import reactor.core.publisher.Mono;

public interface DailyBalanceService {
    Mono<Void> registerDailyBalanceByBankAccount(BankAccount bankAccount);
    Mono<Void> registerDailyBalanceByCreditCard(CreditCard creditCard);
    Mono<Void> registerDailyBalanceByCredit(Credit credit);
}
