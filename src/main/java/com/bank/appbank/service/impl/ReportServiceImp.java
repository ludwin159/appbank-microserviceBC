package com.bank.appbank.service.impl;

import com.bank.appbank.client.MovementServiceClient;
import com.bank.appbank.dto.MovementDto;
import static com.bank.appbank.dto.MovementDto.TypeMovement.*;
import com.bank.appbank.exceptions.ClientNotFoundException;
import com.bank.appbank.model.*;

import static com.bank.appbank.model.BankAccount.TypeBankAccount.*;

import com.bank.appbank.repository.*;
import com.bank.appbank.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReportServiceImp implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private final BankAccountRepository bankAccountRepository;
    private final CreditCardRepository creditCardRepository;
    private final CreditRepository creditRepository;
    private final DailyBalanceRepository dailyBalanceRepository;
    private final ClientRepository clientRepository;
    private final MovementServiceClient movementServiceClient;
//    private final DateProvider dateProvider;
    private final Clock clock;

    public ReportServiceImp(BankAccountRepository bankAccountRepository,
                            CreditCardRepository creditCardRepository,
                            CreditRepository creditRepository,
                            DailyBalanceRepository dailyBalanceRepository,
                            ClientRepository clientRepository,
                            MovementServiceClient movementServiceClient,
//                            DateProvider dateProvider,
                            Clock clock) {
        this.bankAccountRepository = bankAccountRepository;
        this.creditCardRepository = creditCardRepository;
        this.creditRepository = creditRepository;
        this.dailyBalanceRepository = dailyBalanceRepository;
        this.clientRepository = clientRepository;
        this.movementServiceClient = movementServiceClient;
//        this.dateProvider = dateProvider;
        this.clock = clock;
    }


    @Override
    public Mono<Map<String, Object>> generateReportAverageBalanceDailyInPresentMonth(String idClient) {
        log.info("REPORT GENERATE generateReportAverageBalanceDailyInPresentMonth" );
        log.info("idClient: "+idClient);
        Mono<List<Map<String, Object>>> bankAccountFlux = bankAccountRepository.findAllByIdClient(idClient)
                .flatMap(bankAccount -> groupDailyBalanceByBankAccountInPresentMonth(bankAccount.getId())
                        .map(dailyBalances -> {
                            int countActiveDays = dailyBalances.size();
                            if(!todayExistInBalanceDaily(dailyBalances)) {
                                addBalanceActualToDailyBalances(dailyBalances, bankAccount);
                                countActiveDays++;
                            }
                            double sumDailyBalance = sumDailyBalance(dailyBalances);
                            Map<String, Object> bankAccountResponse = new HashMap<>();
                            bankAccountResponse.put("bankAccountId", bankAccount.getId());
                            bankAccountResponse.put("typeBankAccount", bankAccount.getTypeBankAccount());
                            bankAccountResponse.put("currentBalance", bankAccount.getBalance());
                            bankAccountResponse.put("averageDailyBalance", countActiveDays != 0 ?
                                    sumDailyBalance/countActiveDays : 0);
                            return bankAccountResponse;
                        }))
                .collectList();
        Mono<List<Map<String, Object>>> creditCardFlux = creditCardRepository.findAllByIdClient(idClient)
                .flatMap(creditCard -> groupDailyBalanceByBankAccountInPresentMonth(creditCard.getId())
                        .map(dailyBalances -> {
                            int countActiveDays = dailyBalances.size();
                            if(!todayExistInBalanceDaily(dailyBalances)) {
                                addBalanceActualToDailyBalances(dailyBalances, creditCard);
                                countActiveDays++;
                            }
                            double sumDailyBalance = sumDailyBalance(dailyBalances);
                            Map<String, Object> bankAccountResponse = new HashMap<>();
                            bankAccountResponse.put("bankAccountId", creditCard.getId());
                            bankAccountResponse.put("currentBalance", creditCard.getAvailableBalance());
                            bankAccountResponse.put("averageDailyBalance", countActiveDays != 0 ?
                                    sumDailyBalance/countActiveDays : 0);
                            return bankAccountResponse;
                        }))
                .collectList();
        Mono<List<Map<String, Object>>> creditFlux = creditRepository.findAllByIdClient(idClient)
                .flatMap(credit -> groupDailyBalanceByBankAccountInPresentMonth(credit.getId())
                        .map(dailyBalances -> {
                            int countActiveDays = dailyBalances.size();
                            if(!todayExistInBalanceDaily(dailyBalances)) {
                                addBalanceActualToDailyBalances(dailyBalances, credit);
                                countActiveDays++;
                            }
                            double sumDailyBalance = sumDailyBalance(dailyBalances);
                            Map<String, Object> bankAccountResponse = new HashMap<>();
                            bankAccountResponse.put("bankAccountId", credit.getId());
                            bankAccountResponse.put("currentBalance", credit.getPendingBalance());
                            bankAccountResponse.put("averageDailyBalance", countActiveDays != 0 ?
                                    sumDailyBalance/countActiveDays : 0);
                            return bankAccountResponse;
                        }))
                .collectList();


        return Mono.zip(bankAccountFlux, creditCardFlux, creditFlux)
                .flatMap(dataCombine -> {
                    List<Map<String, Object>> bankAccounts = dataCombine.getT1();
                    List<Map<String, Object>> creditCards = dataCombine.getT2();
                    List<Map<String, Object>> credits = dataCombine.getT3();
                    return clientRepository.findById(idClient)
                            .switchIfEmpty(Mono.error(new ClientNotFoundException("The client not found")))
                            .map(client -> {
                                Map<String, Object> report = new HashMap<>();
                                report.put("client", client);
                                report.put("bankAccounts", bankAccounts);
                                report.put("creditCards", creditCards);
                                report.put("credits", credits);
                                return (report);
                            });
                });
    }

    private Mono<List<DailyBalance>> groupDailyBalanceByBankAccountInPresentMonth(String bankAccountId) {
        LocalDateTime to = LocalDateTime.now(clock);
        LocalDateTime from = to.withDayOfMonth(1).toLocalDate().atStartOfDay();

        log.info("from: "+from);
        log.info("to: "+to);
        return dailyBalanceRepository.findAllByIdBankProductAndDateBetween(bankAccountId, from, to)
                .collectList();
    }

    private boolean todayExistInBalanceDaily(List<DailyBalance> dailyBalances) {
        LocalDate today = LocalDate.now(clock);
        log.warn("Reference time: " +today);
        return dailyBalances.stream()
                .map(DailyBalance::getDate)
                .anyMatch(date -> date.equals(today));
    }
    private void addBalanceActualToDailyBalances(List<DailyBalance> dailyBalances, BankProduct bankProduct) {
        double balance = 0.0;
        if (bankProduct instanceof BankAccount) {
            balance = ((BankAccount) bankProduct).getBalance();
        } else if (bankProduct instanceof CreditCard) {
            balance = ((CreditCard) bankProduct).getAvailableBalance();
        } else if (bankProduct instanceof Credit) {
            balance = ((Credit) bankProduct).getPendingBalance();
        }
        DailyBalance dailyBalance = DailyBalance.builder()
                .idBankProduct(bankProduct.getId())
                .date(LocalDate.now())
                .balance(balance)
                .build();
        dailyBalances.add(dailyBalance);
    }

    private double sumDailyBalance(List<DailyBalance> dailyBalances) {
        return dailyBalances.stream()
                .mapToDouble(DailyBalance::getBalance)
                .sum();
    }

    @Override
    public Mono<Map<String, Object>> generateReportAllCommissionsByProductInRangeDate(String from, String to) {
        log.info("REPORT GENERATE generateReportAllCommissionsByProductInRangeDate" );
        log.info("from: "+from);
        log.info("to: "+to);
        return movementServiceClient.getAllMovementsByRangeDate(from, to)
                .onErrorResume(err -> {
                    log.error("An error has occurred" + err);
                    return Mono.error(new RuntimeException("Service not available"));
                })
                .filter(movement -> {
                    log.info(String.valueOf(movement));

                    return movement.getCommissionAmount() > 0 && movement.getTypeMovement() != TRANSFER;
                })
                .collectList()
                .flatMap(this::getBankAccountsAndGenerateReport);
    }
    private Mono<Map<String, Object>> getBankAccountsAndGenerateReport(List<MovementDto> movements) {
        Set<String> bankAccountIds = movements.stream()
                .map(MovementDto::getIdBankAccount)
                .collect(Collectors.toSet());

        return bankAccountRepository.findAllById(bankAccountIds)
                .collectList()
                .map(bankAccounts -> Map.of(
                        "savingAccount", calculateTotalCommission(bankAccounts, movements, SAVING_ACCOUNT),
                        "currentAccount", calculateTotalCommission(bankAccounts, movements, CURRENT_ACCOUNT),
                        "fixedTermAccount", calculateTotalCommission(bankAccounts, movements, FIXED_TERM_ACCOUNT)));
    }
    private double calculateTotalCommission(List<BankAccount> bankAccounts,
                                            List<MovementDto> movements,
                                            BankAccount.TypeBankAccount typeBankAccount) {
        return bankAccounts.stream()
                .filter(bankAccount -> bankAccount.getTypeBankAccount() == typeBankAccount)
                .flatMap(bankAccount -> movements.stream()
                        .filter(movement -> movement.getIdBankAccount().equals(bankAccount.getId())))
                .mapToDouble(MovementDto::getCommissionAmount)
                .sum();
    }
}
