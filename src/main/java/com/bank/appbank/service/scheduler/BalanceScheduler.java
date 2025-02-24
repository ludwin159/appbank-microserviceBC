package com.bank.appbank.service.scheduler;

import com.bank.appbank.model.BankAccount;
import com.bank.appbank.model.Client;
import com.bank.appbank.repository.BankAccountRepository;
import com.bank.appbank.repository.ClientRepository;
import com.bank.appbank.repository.CreditCardRepository;
import com.bank.appbank.repository.CreditRepository;
import com.bank.appbank.service.DailyBalanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class BalanceScheduler {
    private static final Logger log = LoggerFactory.getLogger(BalanceScheduler.class);
    private final DailyBalanceService dailyBalanceService;
    private final BankAccountRepository bankAccountRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditRepository creditRepository;

    public BalanceScheduler(DailyBalanceService dailyBalanceService,
                            BankAccountRepository bankAccountRepository,
                            CreditCardRepository creditCardRepository,
                            CreditRepository creditRepository) {
        this.dailyBalanceService = dailyBalanceService;
        this.bankAccountRepository = bankAccountRepository;
        this.creditCardRepository = creditCardRepository;
        this.creditRepository = creditRepository;
    }

    @PostConstruct
    public void startRecordDailyBalance() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime hourExecution = now.withHour(23).withMinute(59).withSecond(0);
        if (now.isAfter(hourExecution)) {
            log.info("The execution of record daily balance pass for next day");
            hourExecution = hourExecution.plusDays(1);
        }

        long initialDelay = Duration.between(now, hourExecution).toMillis();
        long period = Duration.ofDays(1).toMillis();

        Flux.interval(Duration.ofMillis(initialDelay), Duration.ofMillis(period))
                .flatMap(hour -> {
                    log.info("Start to save daily balance");
                    Flux<Void> bankAccountRegister =  bankAccountRepository.findAll()
                            .flatMap(dailyBalanceService::registerDailyBalanceByBankAccount);
                    Flux<Void> creditCardRegister = creditCardRepository.findAll()
                            .flatMap(dailyBalanceService::registerDailyBalanceByCreditCard);
                    Flux<Void> creditRegister = creditRepository.findAll()
                            .flatMap(dailyBalanceService::registerDailyBalanceByCredit);

                    return Flux.merge(bankAccountRegister, creditCardRegister, creditRegister);
                })
                .onErrorContinue((error, element) ->
                        log.error("An error occur when saving balance error: "+ element + "->" + error.getMessage()))
                .subscribe();
    }


}
