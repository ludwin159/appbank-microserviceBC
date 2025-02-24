package com.bank.appbank.service.impl;

import com.bank.appbank.model.BankAccount;
import com.bank.appbank.model.Credit;
import com.bank.appbank.model.CreditCard;
import com.bank.appbank.model.DailyBalance;
import com.bank.appbank.repository.DailyBalanceRepository;
import com.bank.appbank.service.DailyBalanceService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
public class DailyBalanceServiceImp implements DailyBalanceService {
    private final DailyBalanceRepository dailyBalanceRepository;

    public DailyBalanceServiceImp(DailyBalanceRepository dailyBalanceRepository) {
        this.dailyBalanceRepository = dailyBalanceRepository;
    }

    @Override
    public Mono<Void> registerDailyBalanceByBankAccount(BankAccount bankAccount) {
        DailyBalance dailyBalance = DailyBalance.builder()
                .idBankProduct(bankAccount.getId())
                .balance(bankAccount.getBalance())
                .date(LocalDate.now())
                .build();
        return dailyBalanceRepository.save(dailyBalance)
                .then();
    }

    @Override
    public Mono<Void> registerDailyBalanceByCreditCard(CreditCard creditCard) {
        DailyBalance dailyBalance = DailyBalance.builder()
                .idBankProduct(creditCard.getId())
                .balance(creditCard.getAvailableBalance())
                .date(LocalDate.now())
                .build();
        return dailyBalanceRepository.save(dailyBalance)
                .then();
    }

    @Override
    public Mono<Void> registerDailyBalanceByCredit(Credit credit) {
        DailyBalance dailyBalance = DailyBalance.builder()
                .idBankProduct(credit.getId())
                .balance(credit.getPendingBalance())
                .date(LocalDate.now())
                .build();
        return dailyBalanceRepository.save(dailyBalance)
                .then();
    }
}
