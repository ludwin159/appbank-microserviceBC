package com.bank.appbank.service.impl;

import com.bank.appbank.client.MovementServiceClient;
import com.bank.appbank.dto.MovementDto;
import com.bank.appbank.model.*;
import com.bank.appbank.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.management.ObjectName;
import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ReportServiceImpTest {

    @InjectMocks
    private ReportServiceImp reportService;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private CreditRepository creditRepository;
    @Mock
    private DailyBalanceRepository dailyBalanceRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private MovementServiceClient movementServiceClient;
    @Mock
    private Clock clock;

    private BankAccount bankAccount1, bankAccount2;
    private Credit credit1;
    private CreditCard creditCard1;
    private Client personalClient;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        bankAccount1 = new BankAccount("clientN001",
                1500.0,
                BankAccount.TypeBankAccount.SAVING_ACCOUNT,
                2,
                0,
                0.0,
                20.0,
                0.5,
                5,
                Collections.emptyList(),
                Collections.emptyList());
        bankAccount1.setCreatedAt(LocalDateTime.now(clock).minusDays(2));
        bankAccount1.setId("IDbank001");

        bankAccount2 = new BankAccount("clientN002",
                1200.0,
                BankAccount.TypeBankAccount.CURRENT_ACCOUNT,
                0,
                0,
                15.0,
                0.2,
                0.5,
                5,
                Collections.emptyList(),
                Collections.emptyList());
        bankAccount2.setId("IDbank002");
        bankAccount2.setCreatedAt(LocalDateTime.now(clock).minusDays(3));

        credit1 = new Credit(
                "clientN001",
                500.0,
                200.0,
                0.15,
                LocalDate.now(),
                LocalDate.now(),
                12,
                0.0
        );
        credit1.setId("CREDIT001");

        creditCard1 = new CreditCard();
        creditCard1.setId("CREDIT_CARD001");
        creditCard1.setIdClient("clientN001");
        creditCard1.setLimitCredit(1000.0);
        creditCard1.setAvailableBalance(500.0);

        personalClient = new Client();
        personalClient.setId("clientN001");
        personalClient.setAddress("Jr. avenida");
        personalClient.setTypeClient(Client.TypeClient.PERSONAL_CLIENT);
        personalClient.setEmail("ejemplo@ejemplo.com");
        personalClient.setBusinessName("");
        personalClient.setPhone("");
        personalClient.setTaxId("");
        personalClient.setFullName("Lucas Juan");
        personalClient.setIdentity("75690210");
    }

    @Test
    @DisplayName("Generate a daily report basic")
    void generateReportAverageBalanceDailyInPresentMonth() {
        String idClient = "clientN001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        // Given
        when(bankAccountRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(bankAccount1));
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(creditCard1));
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(credit1));
        when(clientRepository.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(dailyBalanceRepository.findAllByIdBankProductAndDateBetween(eq(bankAccount1.getId()), any(), any()))
                .thenReturn(Flux.empty());
        when(dailyBalanceRepository.findAllByIdBankProductAndDateBetween(eq(creditCard1.getId()), any(), any()))
                .thenReturn(Flux.empty());
        when(dailyBalanceRepository.findAllByIdBankProductAndDateBetween(eq(credit1.getId()), any(), any()))
                .thenReturn(Flux.empty());

        // When
        Mono<Map<String, Object>> mapMono = reportService.generateReportAverageBalanceDailyInPresentMonth(idClient);
        // Then
        StepVerifier.create(mapMono)
                .expectNextMatches(response -> response.size() == 4)
                .verifyComplete();
    }

    @Test
    @DisplayName("Generate a daily report with balance today")
    void generateReportAverageBalanceDailyInPresentMonthWithToday() {
        String idClient = "clientN001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        DailyBalance dailyBalance1 = DailyBalance.builder()
                .idBankProduct(bankAccount1.getId())
                .date(LocalDate.of(2025, 2, 19))
                .balance(130.0)
                .build();
        DailyBalance dailyBalance2 = DailyBalance.builder()
                .idBankProduct(bankAccount1.getId())
                .date(LocalDate.of(2025, 2, 20))
                .balance(690.0)
                .build();

        DailyBalance dailyBalance3 = DailyBalance.builder()
                .idBankProduct(creditCard1.getId())
                .date(LocalDate.of(2025, 2, 20))
                .balance(10.0)
                .build();

        DailyBalance dailyBalance4 = DailyBalance.builder()
                .idBankProduct(credit1.getId())
                .date(LocalDate.of(2025, 2, 19))
                .balance(100.0)
                .build();
        DailyBalance dailyBalance5 = DailyBalance.builder()
                .idBankProduct(credit1.getId())
                .date(LocalDate.of(2025, 2, 20))
                .balance(600.0)
                .build();

        // Given

        when(bankAccountRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(bankAccount1));
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(creditCard1));
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(credit1));
        when(clientRepository.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(dailyBalanceRepository
                .findAllByIdBankProductAndDateBetween(eq(bankAccount1.getId()), any(), any()))
                .thenReturn(Flux.just(dailyBalance1, dailyBalance2));
        when(dailyBalanceRepository
                .findAllByIdBankProductAndDateBetween(eq(creditCard1.getId()), any(), any()))
                .thenReturn(Flux.just(dailyBalance3));
        when(dailyBalanceRepository
                .findAllByIdBankProductAndDateBetween(eq(credit1.getId()), any(), any()))
                .thenReturn(Flux.just(dailyBalance4, dailyBalance5));

        // When
        Mono<Map<String, Object>> mapMono = reportService.generateReportAverageBalanceDailyInPresentMonth(idClient);
        // Then
        StepVerifier.create(mapMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(response.get("client"), personalClient);

                    List<Map<String, Object>> bankAccounts = (List<Map<String, Object>>) response.get("bankAccounts");
                    List<Map<String, Object>> creditCards = (List<Map<String, Object>>) response.get("creditCards");
                    List<Map<String, Object>> credits = (List<Map<String, Object>>) response.get("credits");
                    assertNotNull(bankAccounts);
                    assertEquals(1, bankAccounts.size());
                    assertEquals(1500.0, bankAccounts.get(0).get("currentBalance"));
                    assertEquals(410.0, bankAccounts.get(0).get("averageDailyBalance"));
                    assertEquals(creditCard1.getId(), creditCards.get(0).get("bankAccountId"));
                    assertEquals(creditCard1.getAvailableBalance(), creditCards.get(0).get("currentBalance"));
                    assertEquals(10.0, creditCards.get(0).get("averageDailyBalance"));
                    assertEquals(credit1.getId(), credits.get(0).get("bankAccountId"));
                    assertEquals(credit1.getPendingBalance(), credits.get(0).get("currentBalance"));
                    assertEquals(350.0, credits.get(0).get("averageDailyBalance"));
                })
                .verifyComplete();

    }

    @Test
    @DisplayName("Generate a daily report without balance today")
    void generateReportAverageBalanceDailyInPresentMonthWithoutToday() {
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-21T12:15:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        String idClient = "clientN001";
        DailyBalance dailyBalance1 = DailyBalance.builder()
                .idBankProduct(bankAccount1.getId())
                .date(LocalDate.of(2025, 2, 19))
                .balance(130.0)
                .build();
        DailyBalance dailyBalance2 = DailyBalance.builder()
                .idBankProduct(bankAccount1.getId())
                .date(LocalDate.of(2025, 2, 20))
                .balance(690.0)
                .build();

        DailyBalance dailyBalance3 = DailyBalance.builder()
                .idBankProduct(creditCard1.getId())
                .date(LocalDate.of(2025, 2, 20))
                .balance(10.0)
                .build();

        DailyBalance dailyBalance4 = DailyBalance.builder()
                .idBankProduct(credit1.getId())
                .date(LocalDate.of(2025, 2, 20))
                .balance(100.0)
                .build();

        // Given
        when(bankAccountRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(bankAccount1));
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(creditCard1));
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(credit1));
        when(clientRepository.findById(idClient)).thenReturn(Mono.just(personalClient));

        when(dailyBalanceRepository
                .findAllByIdBankProductAndDateBetween(eq(bankAccount1.getId()), any(), any()))
                .thenReturn(Flux.just(dailyBalance1, dailyBalance2));
        when(dailyBalanceRepository
                .findAllByIdBankProductAndDateBetween(eq(creditCard1.getId()), any(), any()))
                .thenReturn(Flux.just(dailyBalance3));
        when(dailyBalanceRepository
                .findAllByIdBankProductAndDateBetween(eq(credit1.getId()), any(), any()))
                .thenReturn(Flux.just(dailyBalance4));

        // When
        Mono<Map<String, Object>> mapMono = reportService.generateReportAverageBalanceDailyInPresentMonth(idClient);
        // Then
        StepVerifier.create(mapMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(response.get("client"), personalClient);

                    List<Map<String, Object>> bankAccounts = (List<Map<String, Object>>)response.get("bankAccounts");
                    List<Map<String, Object>> creditCards = (List<Map<String, Object>>)response.get("creditCards");
                    List<Map<String, Object>> credits = (List<Map<String, Object>>)response.get("credits");
                    assertNotNull(bankAccounts);
                    assertEquals(1, bankAccounts.size());
                    assertEquals(1500.0, (double)bankAccounts.get(0).get("currentBalance"), 0.001);
                    assertEquals(773.3333, (double)bankAccounts.get(0).get("averageDailyBalance"), 0.001);
                    assertEquals(creditCard1.getId(), creditCards.get(0).get("bankAccountId"));
                    assertEquals(creditCard1.getAvailableBalance(), creditCards.get(0).get("currentBalance"));
                    assertEquals(255.0, (double)creditCards.get(0).get("averageDailyBalance"), 0.001);
                    assertEquals(credit1.getId(), credits.get(0).get("bankAccountId"));
                    assertEquals(credit1.getPendingBalance(), credits.get(0).get("currentBalance"));
                    assertEquals(150.0, (double)credits.get(0).get("averageDailyBalance"), 0.001);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Commission report by date range")
    void generateReportAllCommissionsByProductInRangeDate() {
        MovementDto movement1 = new MovementDto();
        movement1.setId("IDMOVEMENT01");
        movement1.setIdBankAccount("IDbank001");
        movement1.setAmount(20.0);
        movement1.setCommissionAmount(5.0);
        movement1.setTypeMovement(MovementDto.TypeMovement.DEPOSIT);
        movement1.setDescription("Deposit Movement");
        movement1.setIdBankAccountTransfer("");
        movement1.setIdTransfer("");

        MovementDto movement2 = new MovementDto();
        movement2.setId("IDMOVEMENT02");
        movement2.setIdBankAccount("IDbank002");
        movement2.setAmount(10.0);
        movement2.setCommissionAmount(2.0);
        movement2.setTypeMovement(MovementDto.TypeMovement.WITHDRAWAL);
        movement2.setDescription("Withdrawal Movement");
        movement2.setIdBankAccountTransfer("");
        movement2.setIdTransfer("");

        MovementDto movement3 = new MovementDto();
        movement3.setId("IDMOVEMENT03");
        movement3.setIdBankAccount("IDbank001");
        movement3.setAmount(20.0);
        movement3.setCommissionAmount(10.0);
        movement3.setTypeMovement(MovementDto.TypeMovement.DEPOSIT);
        movement3.setDescription("Deposit Movement");
        movement3.setIdBankAccountTransfer("");
        movement3.setIdTransfer("");

        Set<String> idsBankAccount = Set.of(movement2.getIdBankAccount(), movement1.getIdBankAccount());

        // Given
        when(movementServiceClient.getAllMovementsByRangeDate(any(), any()))
                .thenReturn(Flux.just(movement1, movement2, movement3));
        when(bankAccountRepository.findAllById((idsBankAccount))).thenReturn(Flux.just(bankAccount1, bankAccount2));
        // When
        Mono<Map<String, Object>> report = reportService
                .generateReportAllCommissionsByProductInRangeDate("2025-02-10", "2025-02-20");
        // Then
        StepVerifier.create(report)
                .assertNext(result -> {
                    assertNotNull(result);
                    assertThat(result.get("currentAccount")).isEqualTo(2.0);
                    assertThat(result.get("fixedTermAccount")).isEqualTo(0.0);
                    assertThat(result.get("savingAccount")).isEqualTo(15.0);

                    System.out.println(result);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Generate report general of all bank products in range date")
    public void generateReportCompleteAndGeneralByProductInRangeDateTest() {
        // Given
        LocalDate from = LocalDate.now().minusDays(6);
        LocalDate to = LocalDate.now().minusDays(1);
        Instant fromInstant = from.atStartOfDay(clock.getZone()).toInstant();
        Instant toInstant = to.atTime(23, 59, 59).atZone(clock.getZone()).toInstant();
        when(bankAccountRepository
                .findAllByCreatedAtBetween(fromInstant, toInstant)).thenReturn(Flux.just(bankAccount1, bankAccount2));
        when(creditCardRepository
                .findAllByCreatedAtBetween(fromInstant, toInstant)).thenReturn(Flux.just(creditCard1));
        when(creditRepository
                .findAllByCreatedAtBetween(fromInstant, toInstant)).thenReturn(Flux.just(credit1));
        // When
        Mono<Map<String, Object>> result = reportService
                .generateReportCompleteAndGeneralByProductInRangeDate(fromInstant, toInstant);
        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    Map<String, Object> data = (Map<String, Object>)response.get("bankAccounts");
                    assertThat(data.get("total")).isEqualTo(2);
                    List<BankAccount> bankAccounts = null;
                    bankAccounts = (List<BankAccount>)data.get("savingAccounts");
                    assertThat(bankAccounts.size()).isEqualTo(1);
                    bankAccounts = (List<BankAccount>)data.get("fixedTermAccounts");
                    assertEquals(bankAccounts.size(), 0);
                    bankAccounts = (List<BankAccount>)data.get("currentAccounts");
                    assertEquals(bankAccounts.size(), 1);
                })
                .verifyComplete();

    }
}