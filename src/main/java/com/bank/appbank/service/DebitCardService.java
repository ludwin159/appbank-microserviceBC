package com.bank.appbank.service;

import com.bank.appbank.dto.MovementDto;
import com.bank.appbank.model.BankAccountDebitCard;
import com.bank.appbank.model.DebitCard;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface DebitCardService extends ServiceT<DebitCard, String> {
    Mono<DebitCard> update(String idDebitCard, DebitCard debitCard);
    Mono<List<MovementDto>> getLastTenMovementsDebitCard(String idClient);
    Mono<Map<String, Object>> getBalanceOfBankAccountInDebitCard(String idDebitCard);
    Mono<Void> deleteDebitCard(String id);
    Mono<BankAccountDebitCard> addBankAccountToDebitCard(BankAccountDebitCard bankAccountDebitCard);
    Mono<DebitCard> findByIdWithBankAccountsOrderByCreatedAt(String idDebitCard);
}
