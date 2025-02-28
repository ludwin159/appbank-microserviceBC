package com.bank.appbank.service;

import com.bank.appbank.dto.MovementDto;
import com.bank.appbank.model.DebitCard;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface DebitCardService extends ServiceT<DebitCard, String> {
<<<<<<< HEAD
    Mono<DebitCard> update(String idDebitCard, DebitCard debitCard);
    Mono<List<MovementDto>> getLastTenMovementsDebitCard(String idClient);
    Mono<Map<String, Object>> getBalanceOfBankAccountInDebitCard(String idDebitCard);
=======
    public Mono<DebitCard> update(String idDebitCard, DebitCard debitCard);

    Flux<DebitCard> findAllByClientId(String clientId);
>>>>>>> d08731159d5f7028eaa5e24176376745c88efd75
}
