package com.bank.appbank.service.impl;

import com.bank.appbank.client.ConsumptionServiceClient;
import com.bank.appbank.client.PaymentServiceClient;
import com.bank.appbank.exceptions.CreditInvalid;
import com.bank.appbank.exceptions.IneligibleClientException;
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.dto.ConsumptionDto;
import com.bank.appbank.model.Credit;
import com.bank.appbank.model.CreditCard;
import com.bank.appbank.dto.PaymentDto;
import com.bank.appbank.repository.ClientRepository;
import com.bank.appbank.repository.CreditCardRepository;
import com.bank.appbank.service.CreditCardService;
import com.bank.appbank.service.CreditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class CreditCardServiceImp extends ServiceGenImp<CreditCard, String> implements CreditCardService {

    private final PaymentServiceClient paymentClientService;
    private final ConsumptionServiceClient consumptionServiceClient;
    private final ClientRepository clientRepository;
    private final CreditService creditService;
    private final Clock clock;
    private static final Logger log = LoggerFactory.getLogger(CreditCardServiceImp.class);

    public CreditCardServiceImp(RepositoryFactory repositoryFactory,
                                PaymentServiceClient paymentClientService,
                                ClientRepository clientRepository,
                                ConsumptionServiceClient consumptionServiceClient,
                                CreditService creditService,
                                Clock clock) {
        super(repositoryFactory);
        this.paymentClientService = paymentClientService;
        this.clientRepository = clientRepository;
        this.consumptionServiceClient = consumptionServiceClient;
        this.creditService = creditService;
        this.clock = clock;
    }

    @Override
    public Mono<CreditCard> findById(String id) {
        return super.findById(id)
                .flatMap(this::loadPaymentAndConsumption);
    }

    @Override
    public Mono<CreditCard> create(CreditCard creditCard) {
        return clientRepository.findById(creditCard.getIdClient())
                .flatMap(client -> {
                    System.out.println(client);
                    return validateIfClientHasOverDueCredit(client.getId())
                            .flatMap(isValidClient -> setNumbersBillingAndDueDate(creditCard));
                })
                .onErrorResume(ResourceNotFoundException.class, ex ->
                        Mono.error(
                                new ResourceNotFoundException(
                                        "The client with id: "+ creditCard.getIdClient() +" does not exit.")))
                .then(super.create(creditCard));
    }

    private Mono<CreditCard> loadPaymentAndConsumption(CreditCard creditCard) {
        Mono<List<PaymentDto>> paymentsMono =
                paymentClientService.findAllPaymentByIdProductCreditAndSortByDate(creditCard.getId())
                        .collectList();
        Mono<List<ConsumptionDto>> consumptionsMono =
                consumptionServiceClient.findAllConsumptionsByIdCreditCardAndSortByDate(creditCard.getId())
                        .collectList();
        return Mono.zip(paymentsMono, consumptionsMono)
                .map(both -> {
                    List<PaymentDto> payment = both.getT1();
                    List<ConsumptionDto> consumption = both.getT2();
                    creditCard.setPayments(payment);
                    creditCard.setConsumptions(consumption);
                    return creditCard;
                });
    }

    @Override
    public Mono<CreditCard> update(String id, CreditCard creditCard) {
        return getRepository().findById(id)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("The count with id: " + id + " doesn't exist!")))
                .flatMap(creditCardFound -> setNumbersBillingAndDueDate(creditCard)
                        .flatMap(creditCard1 -> {
                            creditCard1.setLimitCredit(creditCard.getLimitCredit());
                            creditCard1.setAvailableBalance(creditCard.getAvailableBalance());
                            creditCard1.setTotalDebt(creditCard.getTotalDebt());
                            return getRepository().save(creditCard1);
                        }));
    }
    private Mono<CreditCard> setNumbersBillingAndDueDate(CreditCard creditCard) {
        if (!Objects.equals(creditCard.getNumberBillingDate(), "20")
                && !Objects.equals(creditCard.getNumberBillingDate(), "13")) {
            return Mono.error(new CreditInvalid("The number billing or number due date is different to 20 or 13"));
        }

        creditCard.setNumberBillingDate(creditCard.getNumberBillingDate());
        if (Objects.equals(creditCard.getNumberBillingDate(), "20"))
            creditCard.setNumberDueDate("5");
        if (Objects.equals(creditCard.getNumberBillingDate(), "13"))
            creditCard.setNumberDueDate("28");
        return Mono.just(creditCard);
    }

    public Mono<Boolean> validateIfClientHasOverDueCredit(String idClient) {
        return Mono.zip(
                        findAllCreditCardsByIdClient(idClient).onErrorResume(ex -> Mono.just(Collections.emptyList())),
                        findAllCreditsByIdClient(idClient).onErrorResume(ex -> Mono.just(Collections.emptyList()))
                )
                .flatMap(tuple -> {
                    List<CreditCard> creditCards = tuple.getT1();
                    List<Credit> credits = tuple.getT2();

                    boolean hasOverDueCreditCard = creditCards.stream()
                            .anyMatch(this::isOverdueCreditCard);
                    boolean hasOverDueCredit = credits.stream()
                            .anyMatch(this::isOverdueCreditOnly);

                    if (hasOverDueCredit || hasOverDueCreditCard) {
                        String message = "The client has an overdue debt";
                        log.error(message);
                        return Mono.error(new IneligibleClientException(message));
                    }
                    return Mono.just(true);
                });
    }

    private Mono<List<CreditCard>> findAllCreditCardsByIdClient(String idClient) {
        return ((CreditCardRepository)getRepository()).findAllByIdClient(idClient)
                .collectList()
                .defaultIfEmpty(Collections.emptyList());
    }

    private Mono<List<Credit>> findAllCreditsByIdClient(String idClient) {
        return creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)
                .collectList()
                .defaultIfEmpty(Collections.emptyList());
    }

    private boolean isOverdueCreditCard(CreditCard creditCard) {
        return creditCard.getDueDate() != null &&
                LocalDate.now(clock).isAfter(creditCard.getDueDate()) && creditCard.getTotalDebt() > 0;
    }

    private boolean isOverdueCreditOnly(Credit credit) {
        LocalDate today = LocalDate.now();
        int numberMonth = today.getMonthValue();
        int numberYear = today.getYear();

        boolean hasPaymentInPresentMonth = credit.getPayments().stream()
                .anyMatch(payment -> payment.getMonthCorresponding() == numberMonth
                        && payment.getYearCorresponding() == numberYear);

        LocalDate dueDate = getDateLimitExpected(credit, numberMonth, numberYear);

        return today.isAfter(dueDate) && !hasPaymentInPresentMonth;
    }
    private LocalDate getDateLimitExpected(Credit credit, int month, int year) {
        LocalDate firstPaymentDate = credit.getFirstDatePay();
        return LocalDate.of(year, month, firstPaymentDate.getDayOfMonth());
    }

    @Override
    public Flux<CreditCard> allCreditCardsByIdClientWithPaymentAndConsumption(String idClient) {
        return ((CreditCardRepository)getRepository()).findAllByIdClient(idClient)
                .flatMap(this::loadPaymentAndConsumption);
    }

    @Override
    public Mono<List<Object>> getLastTenPaymentsAndConsumptionsByIdClient(String idClient) {
        return ((CreditCardRepository)getRepository()).findAllByIdClient(idClient)
                .collectList()
                .flatMap(creditCards -> {
                    List<String> idCreditCards = creditCards.stream()
                            .map(CreditCard::getId)
                            .collect(Collectors.toList());
                    return Mono.zip(
                            paymentClientService.lastTenPaymentsByIdCreditCard(idCreditCards)
                                    .defaultIfEmpty(Collections.emptyList()),
                            consumptionServiceClient.findLastTenConsumptions(idCreditCards)
                                    .defaultIfEmpty(Collections.emptyList())
                    ).map(tuple -> {
                        List<PaymentDto> payments = tuple.getT1();
                        List<ConsumptionDto> consumptions = tuple.getT2();
                        List<Object> combinedList = new ArrayList<>();
                        combinedList.addAll(payments);
                        combinedList.addAll(consumptions);
                        combinedList.sort((a, b) -> {
                            LocalDateTime createdAtA = getCreatedAt(a);
                            LocalDateTime createdAtB = getCreatedAt(b);
                            return createdAtB.compareTo(createdAtA);
                        });
                        return combinedList.stream().limit(10).collect(Collectors.toList());
                    });
                });
    }
    private LocalDateTime getCreatedAt(Object element) {
        if (element instanceof PaymentDto) {
            return ((PaymentDto) element).getCreatedAt();
        } else if (element instanceof ConsumptionDto){
            return ((ConsumptionDto) element).getCreatedAt();
        }
        return LocalDateTime.MIN;
    }

    @Override
    protected Class<CreditCard> getEntityClass() {
        return CreditCard.class;
    }
}
