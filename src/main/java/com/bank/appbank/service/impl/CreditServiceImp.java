package com.bank.appbank.service.impl;

import com.bank.appbank.client.PaymentServiceClient;
import com.bank.appbank.exceptions.CanNotDeleteEntity;
import com.bank.appbank.exceptions.CreditInvalid;
import com.bank.appbank.exceptions.IneligibleClientException;
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.Client;
import com.bank.appbank.model.Credit;
import com.bank.appbank.model.CreditCard;
import com.bank.appbank.repository.CreditCardRepository;
import com.bank.appbank.repository.CreditRepository;
import com.bank.appbank.service.ClientService;
import com.bank.appbank.service.CreditService;
import com.bank.appbank.utils.Numbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class CreditServiceImp extends ServiceGenImp<Credit, String> implements CreditService {

    public final ClientService clientService;
    public final PaymentServiceClient paymentServiceClient;
    private final Clock clock;
    private final CreditCardRepository creditCardRepository;
    private static final Logger log = LoggerFactory.getLogger(CreditServiceImp.class);

    public CreditServiceImp(RepositoryFactory repositoryFactory,
                            ClientService clientService,
                            PaymentServiceClient paymentServiceClient,
                            CreditCardRepository creditCardRepository,
                            Clock clock) {
        super(repositoryFactory);
        this.clientService = clientService;
        this.paymentServiceClient = paymentServiceClient;
        this.creditCardRepository = creditCardRepository;
        this.clock = clock;
    }

    @Override
    public Mono<Credit> findById(String id) {
        return super.findById(id)
                .flatMap(credit ->
                        paymentServiceClient.findAllPaymentByIdProductCreditAndSortByDate(id)
                                .collectList()
                                .flatMap(payments ->{
                                    credit.setPayments(payments);
                                    return Mono.just(credit);
                                })
                );
    }

    public Mono<Credit> update(String id, Credit credit) {
        return getRepository().findById(id)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("The Credit with id: " + id + " doesn't exist!")))
                .flatMap(creditFound -> {
//                    creditFound.setIdClient(credit.getIdClient());
                    creditFound.setPendingBalance(credit.getPendingBalance());
                    creditFound.setInterestRate(credit.getInterestRate());
                    creditFound.setTotalAmount(credit.getTotalAmount());
                    creditFound.setFirstDatePay(credit.getFirstDatePay());
                    creditFound.setDisbursementDate(credit.getDisbursementDate());
                    return getRepository().save(creditFound);
                });
    }

    @Override
    public Flux<Credit> findAllCreditsByIdClient(String idClient) {
        return ((CreditRepository)getRepository()).findAllByIdClient(idClient);
    }

    @Override
    public Flux<Credit> allCreditsByIdClientWithAllPaymentsSortedByDatePayment(String idClient) {
        return findAllCreditsByIdClient(idClient)
                .flatMap(credit ->
                        paymentServiceClient.findAllPaymentByIdProductCreditAndSortByDate(credit.getId())
                                .collectList()
                                .map(payments -> {
                                    credit.setPayments(payments);
                                    return credit;
                                }));
    }

    @Override
    public Mono<Credit> create(Credit credit) {
        Mono<Client> clientFound = clientService.findById(credit.getIdClient())
                .flatMap(client -> validateIfClientHasOverDueCredit(client.getId())
                        .flatMap(isValidClient -> Mono.just(client)))
                .onErrorResume(ResourceNotFoundException.class, (exception) ->
                        Mono.error(new ResourceNotFoundException(
                                "The Client with id: " + credit.getIdClient() + " doesn't exist!")));

        Mono<Long> numberCredits = findAllCreditsByIdClient(credit.getIdClient()).count();

        return clientFound.zipWith(numberCredits).flatMap(clientWithCredits -> {
            Client client = clientWithCredits.getT1();
            Long credits = clientWithCredits.getT2();
            if (client.getTypeClient() == Client.TypeClient.PERSONAL_CLIENT && credits > 0) {
                return Mono.error(new CreditInvalid(
                        String.format("The customer %s of type: %s, already has a credit.",
                                client.getFullName(),
                                client.getTypeClient())));
            }
            return validCredit(credit)
                    .flatMap(credit1 -> {
                        putPendingAmountAndGenerateMonthlyFee(credit1);
                        return super.create(credit);
                    });
        });
    }

    private void putPendingAmountAndGenerateMonthlyFee(Credit credit) {
        credit.setMonthlyFee(generateMonthlyFee(credit.getTotalAmount(),
                credit.getInterestRate(), credit.getTotalMonths()));
        credit.setPendingBalance(credit.getTotalAmount());
    }

    private double generateMonthlyFee(double totalAmount, double interestRate, int totalMonth) {
        double interestRateMonthly = interestRate/totalMonth;
        double result = (totalAmount * interestRateMonthly) / (1 - Math.pow(1 + interestRateMonthly, -totalMonth));
        return Numbers.redondear(result);
    }

    private Mono<Credit> validCredit(Credit credit) {
        if (credit.getDisbursementDate().isBefore(LocalDate.now(clock))) {
            return Mono.error(new CreditInvalid("The disbursement date must be greater than the current date."));
        }

        if (credit.getFirstDatePay().isBefore(LocalDate.now(clock))) {
            return Mono.error(new CreditInvalid("The first payment date must be greater than the current date."));
        }
        return Mono.just(credit);
    }
    public Mono<Boolean> validateIfClientHasOverDueCredit(String idClient) {
        return Mono.zip(
                        findAllCreditCardsByIdClient(idClient).onErrorResume(ex -> Mono.just(Collections.emptyList())),
                        findAllCreditsByIdClientWithPayments(idClient)
                                .onErrorResume(ex -> Mono.just(Collections.emptyList()))
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
        return creditCardRepository.findAllByIdClient(idClient)
                .collectList()
                .defaultIfEmpty(Collections.emptyList());
    }

    private Mono<List<Credit>> findAllCreditsByIdClientWithPayments(String idClient) {
        return allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)
                .collectList()
                .defaultIfEmpty(Collections.emptyList());
    }

    private boolean isOverdueCreditCard(CreditCard creditCard) {
        return creditCard.getDueDate() != null &&
                LocalDate.now(clock).isAfter(creditCard.getDueDate()) && creditCard.getTotalDebt() > 0;
    }

    private boolean isOverdueCreditOnly(Credit credit) {
        LocalDate today = LocalDate.now(clock);
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
    public Mono<Void> deleteById(String id) {
        return getRepository().findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("The credit is not found")))
                .flatMap(credit -> {
                    if (credit.getPendingBalance() > 0)
                        return Mono.error(
                                new CanNotDeleteEntity("Credit can not delete while has a pending balance"));
                    return super.deleteById(credit.getId());
                })
                .then(Mono.empty());
    }

    @Override
    protected Class<Credit> getEntityClass() {
        return Credit.class;
    }
}
