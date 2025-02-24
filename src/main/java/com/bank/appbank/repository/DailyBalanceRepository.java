package com.bank.appbank.repository;

import com.bank.appbank.model.DailyBalance;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface DailyBalanceRepository extends ReactiveMongoRepository<DailyBalance, String> {
    Flux<DailyBalance> findAllByIdBankProductAndDateBetween(String idBankProduct, LocalDateTime from, LocalDateTime to);
}
