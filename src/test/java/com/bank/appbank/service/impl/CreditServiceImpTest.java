package com.bank.appbank.service.impl;

import com.bank.appbank.client.PaymentServiceClient;
import com.bank.appbank.exceptions.CanNotDeleteEntity;
import com.bank.appbank.exceptions.CreditInvalid;
import com.bank.appbank.exceptions.IneligibleClientException;
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.Client;
import com.bank.appbank.model.Credit;
import com.bank.appbank.dto.PaymentDto;
import com.bank.appbank.model.CreditCard;
import com.bank.appbank.repository.CreditCardRepository;
import com.bank.appbank.repository.CreditRepository;
import com.bank.appbank.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.*;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CreditServiceImpTest {

    @InjectMocks
    private CreditServiceImp creditService;

    @Mock
    private ClientService clientService;
    @Mock
    private RepositoryFactory repositoryFactory;
    @Mock
    private CreditRepository creditRepository;
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private PaymentServiceClient paymentServiceClient;
    @Mock
    private Clock clock;

    private PaymentDto payment1, payment2;
    private Credit credit1;
    private CreditCard creditCard1;
    private Client personalClient;

    @BeforeEach
    void setUp() {
        when(repositoryFactory.getRepository(any())).thenReturn(creditRepository);

        payment1 = new PaymentDto();
        payment1.setId("PAYMENT001");
        payment1.setIdProductCredit("CREDIT001");
        payment1.setAmount(30.0);
        payment1.setMonthCorresponding(1);
        payment1.setYearCorresponding(2025);
        payment1.setTypeCreditProduct(PaymentDto.TypeCreditProduct.CREDIT);

        payment2 = new PaymentDto();
        payment2.setId("PAYMENT002");
        payment2.setIdProductCredit("CREDIT_CARD001");
        payment2.setAmount(25.0);
        payment2.setTypeCreditProduct(PaymentDto.TypeCreditProduct.CREDIT_CARD);

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
    @DisplayName("Find credits with payments by id")
    void findById() {
        String idCredit = "CREDIT001";
        // Given
        when(creditRepository.findById(idCredit)).thenReturn(Mono.just(credit1));
        when(paymentServiceClient
                .findAllPaymentByIdProductCreditAndSortByDate(idCredit))
                .thenReturn(Flux.just(payment1, payment2));
        // WHen
        Mono<Credit> creditMono = creditService.findById(idCredit);
        // Then
        StepVerifier.create(creditMono)
                .expectNextMatches(credit -> credit.getId().equals(idCredit) && credit.getPayments().size() == 2)
                .verifyComplete();
        verify(creditRepository).findById(idCredit);
        verify(paymentServiceClient).findAllPaymentByIdProductCreditAndSortByDate(idCredit);
    }

    @Test
    @DisplayName("update a credit")
    void updateCreditTest() {
        String idCredit = "CREDIT001";
        Credit creditNew = new Credit(
                "clientN001",
                500.0,
                100.0,
                    0.5,
                LocalDate.now(),
                LocalDate.now(),
                12,
                0.0
        );
        creditNew.setId("CREDIT001");
        // Given
        when(creditRepository.findById(idCredit)).thenReturn(Mono.just(credit1));
        when(creditRepository.save(credit1)).thenReturn(Mono.just(credit1));
        // WHen
        Mono<Credit> creditMono = creditService.update(idCredit, creditNew);
        // Then
        StepVerifier.create(creditMono)
                .expectNextMatches(credit -> credit.getId().equals(idCredit) && credit.getPendingBalance() == 100.0)
                .verifyComplete();
        verify(creditRepository).findById(idCredit);
        verify(creditRepository).save(credit1);
    }

    @Test
    @DisplayName("update a credit when not exist")
    void updateCreditNotExistTest() {
        String idCreditNotExist = "CREDIT001";
        // Given
        when(creditRepository.findById(idCreditNotExist)).thenReturn(Mono.error(new ResourceNotFoundException("")));
        // WHen
        Mono<Credit> creditMono = creditService.update(idCreditNotExist, credit1);
        // Then
        StepVerifier.create(creditMono)
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Get all credits of One client by id")
    void allCreditsByIdClientWithAllPaymentsSortedByDatePayment() {
        String idClient = "clientN001";
        Credit credit2 = new Credit(
                "clientN001",
                100.0,
                20.0,
                0.1,
                LocalDate.now(),
                LocalDate.now(),
                12,
                0.0
        );
        credit2.setId("CREDIT002");
        // Given
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(credit1, credit2));
        when(paymentServiceClient.findAllPaymentByIdProductCreditAndSortByDate(credit1.getId()))
                .thenReturn(Flux.just(payment1, payment2));
        when(paymentServiceClient.findAllPaymentByIdProductCreditAndSortByDate(credit2.getId()))
                .thenReturn(Flux.just(payment1));
        // When
        Flux<Credit> creditFlux = creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient);
        // Then
        StepVerifier.create(creditFlux)
                .expectNextMatches(credit ->
                    credit.getIdClient().equals(idClient)
                            && credit.getId().equals("CREDIT001") && credit.getPayments().size() == 2)
                .expectNextMatches(credit ->
                        credit.getId().equals("CREDIT002")
                                && credit.getTotalAmount() == 100.0 && credit.getPayments().size() == 1)
                .verifyComplete();

        verify(creditRepository).findAllByIdClient(idClient);
        verify(paymentServiceClient).findAllPaymentByIdProductCreditAndSortByDate(credit1.getId());
    }

    @Test
    @DisplayName("Create a credit personal with already credit")
    void createCreditTest() {
        String idClient = "clientN001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        Credit existCredit = new Credit(
                "clientN001",
                2000.0,
                1800.0,
                0.15,
                LocalDate.now(clock),
                LocalDate.now(clock),
                12,
                0.0
        );
        existCredit.setId("CREDIT002");
        // Given
        when(clientService.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(existCredit));
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        // When
        Mono<Credit> creditMono = creditService.create(credit1);
        // Then
        StepVerifier.create(creditMono)
                .expectError(CreditInvalid.class)
                .verify();
        verify(clientService).findById(idClient);
        verify(creditRepository, times(2)).findAllByIdClient(idClient);
    }

    @Test
    @DisplayName("Create a credit personal without credits")
    void createCreditWithoutCreditsTest() {
        String idClient = "clientN001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        // Given
        when(clientService.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(creditRepository.save(credit1)).thenReturn(Mono.just(credit1));
        // When
        Mono<Credit> creditMono = creditService.create(credit1);
        // Then
        StepVerifier.create(creditMono)
                .assertNext(credit -> {
                    assertThat(credit).isNotNull();
                    assertThat(credit.getId()).isEqualTo(credit1.getId());
                    assertThat(credit.getTotalMonths()).isEqualTo(12);
                    assertThat(credit.getMonthlyFee()).isEqualTo(45.13);
                })
                .verifyComplete();
        verify(clientService).findById(idClient);
        verify(creditRepository, times(2)).findAllByIdClient(idClient);
        verify(creditRepository).save(credit1);
    }

    @Test
    @DisplayName("Create a credit with date smallest today")
    void createCreditSmallestTodayTest() {
        String idClient = "clientN001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        credit1 = new Credit(
                "clientN001",
                500.0,
                100.0,
                0.15,
                LocalDate.of(2025, 2, 18),
                LocalDate.of(2025, 3, 2),
                12,
                0.0

        );
        // Given
        when(clientService.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        // When
        Mono<Credit> creditMono = creditService.create(credit1);
        // Then
        StepVerifier.create(creditMono)
                .expectError(CreditInvalid.class)
                .verify();
    }

    @Test
    @DisplayName("Create a credit with date pay is smallest today")
    void createCreditDayPaySmallestTest() {
        String idClient = "clientN001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        credit1 = new Credit(
                "clientN001",
                500.0,
                500.0,
                0.15,
                LocalDate.of(2025, 3, 2),
                LocalDate.of(2025, 2, 18),
                12,
                0.0

        );
        // Given
        when(clientService.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        // When
        Mono<Credit> creditMono = creditService.create(credit1);
        // Then
        StepVerifier.create(creditMono)
                .expectError(CreditInvalid.class)
                .verify();
    }

    @Test
    @DisplayName("Create a credit with due date credit card")
    void createCreditCardDueDateTest() {
        String idClient = "clientN001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        creditCard1.setTotalDebt(1000.0);
        creditCard1.setDueDate(LocalDate.of(2025, 1, 5));
        // Given
        when(clientService.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(creditCard1));
        // When
        Mono<Credit> creditMono = creditService.create(credit1);
        // Then
        StepVerifier.create(creditMono)
                .expectError(IneligibleClientException.class)
                .verify();
    }

    @Test
    @DisplayName("Create a  when client not exist")
    void createCreditWithoutClientTest() {
        String idClientNotExists = "clientN000";
        credit1.setIdClient(idClientNotExists);
        // Given
        when(clientService.findById(idClientNotExists)).thenReturn(Mono.error(new ResourceNotFoundException("")));
        when(creditRepository.findAllByIdClient(idClientNotExists)).thenReturn(Flux.empty());
        // When
        Mono<Credit> creditMono = creditService.create(credit1);
        // Then
        StepVerifier.create(creditMono)
                .expectError(ResourceNotFoundException.class)
                .verify();
    }
    @Test
    @DisplayName("Create a credit with business client")
    void createCreditWithBusinessClientTest() {
        String idClient = "clientN002";
        Client businessClient = new Client();
        businessClient.setId("clientN002");
        businessClient.setAddress("Jr. avenida Jose Pérez");
        businessClient.setTypeClient(Client.TypeClient.BUSINESS_CLIENT);
        businessClient.setEmail("business@ejemplo.com");
        businessClient.setBusinessName("Panadería Boluelos");
        businessClient.setTaxId("20756902101");

        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        credit1 = new Credit(
                idClient,
                500.0,
                200.0,
                0.15,
                LocalDate.now(clock),
                LocalDate.now(clock).plusMonths(1).withDayOfMonth(2),
                12,
                0.0
        );
        credit1.setId("CREDIT001");

        // Given
        when(clientService.findById(idClient)).thenReturn(Mono.just(businessClient));
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(credit1));
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(creditRepository.save(credit1)).thenReturn(Mono.just(credit1));
        // When
        Mono<Credit> creditMono = creditService.create(credit1);
        // Then
        StepVerifier.create(creditMono)
                .assertNext(credit -> {
                    assertThat(credit.getId()).isEqualTo(credit1.getId());
                    assertThat(credit.getMonthlyFee()).isEqualTo(45.13);
                    assertThat(credit.getTotalAmount()).isEqualTo(credit.getPendingBalance());
                })
                .verifyComplete();
        verify(clientService).findById(idClient);
        verify(creditRepository, times(2)).findAllByIdClient(idClient);
        verify(creditRepository).save(credit1);
    }

    @Test
    @DisplayName("Can not delete a credit while its pending balance is more than zero")
    void deleteCreditWhileBalanceMoreThanZero() {
        // Given
        when(creditRepository.findById(credit1.getId())).thenReturn(Mono.just(credit1));
        // When
        Mono<Void> deleting = creditService.deleteById(credit1.getId());
        // Then
        StepVerifier.create(deleting)
                .expectError(CanNotDeleteEntity.class)
                .verify();
        verify(creditRepository).findById(credit1.getId());
    }

    @Test
    @DisplayName("Delete credit")
    void deleteCreditTest() {
        // Given
        credit1.setPendingBalance(0.0);
        when(creditRepository.findById(credit1.getId())).thenReturn(Mono.just(credit1));
        when(creditRepository.deleteById(credit1.getId())).thenReturn(Mono.empty());
        // When
        Mono<Void> deleting = creditService.deleteById(credit1.getId());
        // Then
        StepVerifier.create(deleting)
                .verifyComplete();
        verify(creditRepository).findById(credit1.getId());
        verify(creditRepository).deleteById(credit1.getId());
    }
    @Test
    @DisplayName("Delete credit when that not exist")
    void deleteCreditNotExistTest() {
        // Given
        when(creditRepository.findById(credit1.getId())).thenReturn(Mono.empty());
        // When
        Mono<Void> deleting = creditService.deleteById(credit1.getId());
        // Then
        StepVerifier.create(deleting)
                .expectError(ResourceNotFoundException.class)
                .verify();
        verify(creditRepository).findById(credit1.getId());
    }

    @Test
    @DisplayName("Create a credit with due date credit")
    void createCreditDueDateCreditTest() {
        String idClient = "clientN001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());

        payment1 = new PaymentDto();
        payment1.setDatePayment(LocalDateTime.now(clock));
        payment1.setMonthCorresponding(12);
        payment1.setYearCorresponding(2024);
        payment1.setTypeCreditProduct(PaymentDto.TypeCreditProduct.CREDIT);
        payment1.setAmount(credit1.getTotalAmount());
        payment1.setIdProductCredit(credit1.getId());

        credit1.setDisbursementDate(LocalDate.of(2024, 11, 5));
        credit1.setFirstDatePay(LocalDate.of(2024, 12, 2));

        Credit credit = new Credit(
                "clientN001",
                500.0,
                500.0,
                0.15,
                LocalDate.of(2025, 2, 18),
                LocalDate.of(2025, 3, 2),
                12,
                0.0
        );
        // Given
        when(clientService.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(credit1));
        when(paymentServiceClient.findAllPaymentByIdProductCreditAndSortByDate("CREDIT001"))
                .thenReturn(Flux.just(payment1));
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        // When
        Mono<Credit> creditMono = creditService.create(credit);
        // Then
        StepVerifier.create(creditMono)
                .expectError(IneligibleClientException.class)
                .verify();
    }


}