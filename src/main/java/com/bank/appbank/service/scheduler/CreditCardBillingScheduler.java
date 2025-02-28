package com.bank.appbank.service.scheduler;

import com.bank.appbank.client.ConsumptionServiceClient;
import com.bank.appbank.dto.ConsumptionDto;
import com.bank.appbank.model.CreditCard;
import com.bank.appbank.repository.CreditCardRepository;
import com.bank.appbank.utils.Numbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Component
public class CreditCardBillingScheduler {
    private final static Logger log = LoggerFactory.getLogger(CreditCardBillingScheduler.class);
    private final CreditCardRepository creditCardRepository;
    private final ConsumptionServiceClient consumptionServiceClient;


    public CreditCardBillingScheduler(CreditCardRepository creditCardRepository,
                                      ConsumptionServiceClient consumptionServiceClient) {
        this.creditCardRepository = creditCardRepository;
        this.consumptionServiceClient = consumptionServiceClient;
    }

    @PostConstruct
    public void init() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime executionHour = now.withHour(23).withMinute(0);
        log.info("Configuring scheduler billing credit cards");
        if (now.isAfter(executionHour)) {
            executionHour = executionHour.plusDays(1);
        }
        long initialDelay = Duration.between(now, executionHour).toMillis();
        long period = Duration.ofDays(1).toMillis();

        Flux.interval(Duration.ofMillis(initialDelay), Duration.ofMillis(period))
                .flatMap(tick -> processDaily())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private Flux<CreditCard> processDaily() {
        log.info("Init scheduler billing credit cards: " + LocalDateTime.now());
        return creditCardRepository.findAll()
                .flatMap(creditCard -> {
                    LocalDate today = LocalDate.now();
                    int numberDay = today.getDayOfMonth();

                    if (numberDay == Integer.parseInt(creditCard.getNumberBillingDate())) {
                        return generateBillingStatement(creditCard);
                    }

                    else if (creditCard.getDueDate()!=null &&
                            today.isAfter(creditCard.getDueDate()) &&
                            creditCard.getTotalDebt() > 0) {
                        return applyOverduePenalty(creditCard);
                    }

                    return Mono.just(creditCard);
                })
                .doOnNext(card -> log.info("Processed card: {}", card.getId()))
                .doOnError(error -> log.error("Error in daily processing: ", error));
    }

    private Mono<CreditCard> generateBillingStatement(CreditCard creditCard) {
        LocalDate today = LocalDate.now();
        int billingMonth = today.getMonthValue();
        int billingYear = today.getYear();

        return consumptionServiceClient.findByIdCreditCardAndBilledFalse(creditCard.getId())
                .collectList()
                .flatMap(consumptions -> {
                    double newDebt = consumptions.stream()
                            .mapToDouble(ConsumptionDto::getAmount)
                            .sum();

                    consumptions.forEach(consumption -> {
                        consumption.setBilled(true);
                        consumption.setBillingMonth(billingMonth);
                        consumption.setBillingYear(billingYear);
                    });

                    creditCard.setTotalDebt(creditCard.getTotalDebt() + newDebt);

//                    creditCard.setAvailableBalance(creditCard.getAvailableBalance() - newDebt);

                    LocalDate newDueDate = today.plusMonths(1)
                            .withDayOfMonth(Integer.parseInt(creditCard.getNumberDueDate()));
                    creditCard.setDueDate(newDueDate);

                    log.info("Billing statement generated for card: {}", creditCard.getId());

                    return consumptionServiceClient.saveAll(consumptions)
                            .then(creditCardRepository.save(creditCard));
                });
    }

    private Mono<CreditCard> applyOverduePenalty(CreditCard creditCard) {
        double interestPenalty = 0.15;
        double dailyInterest = interestPenalty / 30;
        double penalty = Numbers.redondear(creditCard.getTotalDebt() * dailyInterest);

        creditCard.setTotalDebt(creditCard.getTotalDebt() + penalty);

        creditCard.setAvailableBalance(creditCard.getLimitCredit() - creditCard.getTotalDebt());

        log.info("Applied penalty of {} to card {}", penalty, creditCard.getId());

        return creditCardRepository.save(creditCard);
    }
}
