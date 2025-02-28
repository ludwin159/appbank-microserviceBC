package com.bank.appbank.service;

import com.bank.appbank.dto.MovementDto;
import com.bank.appbank.model.DebitCard;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface DebitCardService extends ServiceT<DebitCard, String> {
    Mono<DebitCard> update(String idDebitCard, DebitCard debitCard);
    Mono<List<MovementDto>> getLastTenMovementsDebitCard(String idClient);
    Mono<Map<String, Object>> getBalanceOfBankAccountInDebitCard(String idDebitCard);
}
