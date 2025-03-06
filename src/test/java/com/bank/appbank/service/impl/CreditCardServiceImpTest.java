package com.bank.appbank.service.impl;

import com.bank.appbank.client.ConsumptionServiceClient;
import com.bank.appbank.client.PaymentServiceClient;
import com.bank.appbank.dto.ConsumptionDto;
import com.bank.appbank.exceptions.IneligibleClientException;
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.Client;
import com.bank.appbank.model.Credit;
import com.bank.appbank.model.CreditCard;
import com.bank.appbank.dto.PaymentDto;
import com.bank.appbank.repository.ClientRepository;
import com.bank.appbank.repository.CreditCardRepository;
import com.bank.appbank.service.ClientService;
import com.bank.appbank.service.CreditService;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceImpTest {

    @InjectMocks
    private CreditCardServiceImp creditCardService;
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private PaymentServiceClient paymentService;
    @Mock
    private ConsumptionServiceClient consumptionService;
    @Mock
    private ClientService clientService;
    @Mock
    private RepositoryFactory repositoryFactory;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private Clock clock;
    @Mock
    private CreditService creditService;

    private PaymentDto payment1, payment2;
    private Credit credit1;
    private CreditCard creditCard1;
    private Client personalClient;

    @BeforeEach
    void setUp() {

        payment1 = new PaymentDto();
        payment1.setId("PAYMENT001");
        payment1.setIdProductCredit("CREDIT001");
        payment1.setAmount(30.0);
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
        creditCard1.setNumberBillingDate("20");
        creditCard1.setNumberDueDate("5");

        personalClient = new Client();
        personalClient.setId("clientN001");
        personalClient.setAddress("Jr. avenida");
        personalClient.setTypeClient(Client.TypeClient.PERSONAL_CLIENT);
        personalClient.setBusinessName("");
        personalClient.setPhone("");
        personalClient.setTaxId("");
        personalClient.setEmail("ejemplo@ejemplo.com");
        personalClient.setFullName("Lucas Juan");
        personalClient.setIdentity("75690210");
    }

    @Test
    @DisplayName("Find a credit card and get its consumptions and payments")
    void findById() {
        String creditCardId = "CREDIT_CARD001";
        // Given
        when(repositoryFactory.getRepository(any())).thenReturn(creditCardRepository);
        when(creditCardRepository.findById(creditCardId)).thenReturn(Mono.just(creditCard1));
        when(paymentService.findAllPaymentByIdProductCreditAndSortByDate(creditCardId))
                .thenReturn(Flux.just(payment1, payment2));
        when(consumptionService.findAllConsumptionsByIdCreditCardAndSortByDate(creditCardId)).thenReturn(Flux.empty());
        // When
        Mono<CreditCard> creditCardMono = creditCardService.findById(creditCardId);
        // Then
        StepVerifier.create(creditCardMono)
                .expectNextMatches(creditCard ->
                        creditCard.getId().equals(creditCardId) && creditCard.getConsumptions().isEmpty()
                    && creditCard.getPayments().size() == 2)
                .verifyComplete();
        verify(creditCardRepository).findById(creditCardId);
        verify(paymentService).findAllPaymentByIdProductCreditAndSortByDate(creditCardId);
        verify(consumptionService).findAllConsumptionsByIdCreditCardAndSortByDate(creditCardId);
    }

    @Test
    @DisplayName("Create a credit card with exist client")
    void createCreditCardTest() {
        String idClient = "clientN001";
        // Given
        when(repositoryFactory.getRepository(any())).thenReturn(creditCardRepository);
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(clientRepository.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(creditCardRepository.save(creditCard1)).thenReturn(Mono.just(creditCard1));
        // WHen
        Mono<CreditCard> creditCardMono = creditCardService.create(creditCard1);
        // Then
        StepVerifier.create(creditCardMono)
                .expectNextMatches(creditCard -> creditCard.getId().equals(creditCard1.getId()))
                .verifyComplete();
        verify(clientRepository).findById(idClient);
        verify(creditCardRepository).save(creditCard1);
    }

    @Test
    @DisplayName("Create a credit card when client not exist")
    void createCreditCardWithNotExistClientTest() {
        String idClient = "clientN001";
        // Given
        when(clientRepository.findById(idClient)).thenReturn(Mono.error(new ResourceNotFoundException("")));
        // WHen
        Mono<CreditCard> creditCardMono = creditCardService.create(creditCard1);
        // Then
        StepVerifier.create(creditCardMono)
                .expectError(ResourceNotFoundException.class)
                .verify();
        verify(clientRepository).findById(idClient);
    }

    @Test
    @DisplayName("Update credit card Test")
    void update() {
        String idClient = "clientN001";
        CreditCard creditCardNew = new CreditCard();
        creditCardNew.setId("CREDIT_CARD001");
        creditCardNew.setIdClient(idClient);
        creditCardNew.setLimitCredit(500.0);
        creditCardNew.setAvailableBalance(50.0);
        creditCardNew.setNumberBillingDate("20");
        creditCardNew.setNumberDueDate("5");
        // Given
        when(repositoryFactory.getRepository(any())).thenReturn(creditCardRepository);
        when(creditCardRepository.findById("CREDIT_CARD001")).thenReturn(Mono.just(creditCard1));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(Mono.just(creditCardNew));
        // WHen
        Mono<CreditCard> creditCardMono = creditCardService.update("CREDIT_CARD001", creditCardNew);
        // Then
        StepVerifier.create(creditCardMono)
                .expectNextMatches(creditCard -> creditCard.getId().equals("CREDIT_CARD001")
                && creditCard.getLimitCredit() == 500.0)
                .verifyComplete();
        verify(creditCardRepository).findById("CREDIT_CARD001");
    }

    @Test
    @DisplayName("Update credit card when not exists")
    void updateCreditCardWhenNotExistsTest() {
        String idClient = "clientN001";
        // Given
        when(repositoryFactory.getRepository(any())).thenReturn(creditCardRepository);
        when(creditCardRepository.findById(idClient)).thenReturn(Mono.error(new ResourceNotFoundException("")));
        // WHen
        Mono<CreditCard> creditCardMono = creditCardService.update("clientN001", creditCard1);
        // Then
        StepVerifier.create(creditCardMono)
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Get all credit cards with payment and consumption by client id")
    void allCreditCardsByIdClientWithPaymentAndConsumption() {

        String idClient = "clientN001";
        when(repositoryFactory.getRepository(any())).thenReturn(creditCardRepository);
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(creditCard1));
        when(paymentService.findAllPaymentByIdProductCreditAndSortByDate(creditCard1.getId()))
                .thenReturn(Flux.just(payment2, payment2));
        when(consumptionService.findAllConsumptionsByIdCreditCardAndSortByDate(creditCard1.getId()))
                .thenReturn(Flux.empty());

        StepVerifier.create(creditCardService.allCreditCardsByIdClientWithPaymentAndConsumption(idClient))
                .expectNextMatches(creditCard -> creditCard.getPayments().size() == 2
                        && creditCard.getConsumptions().isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Get last ten Payments and consumptions by client id")
    void getLastTenPaymentsAndConsumptionsByIdClientTest() {
        String idClient = personalClient.getId();
        List<String> idCreditCards = List.of(creditCard1.getId());
        ConsumptionDto consumption1 = new ConsumptionDto();
        consumption1.setId("CONSUMPTION_1");
        consumption1.setAmount(20.0);
        consumption1.setCreatedAt(LocalDateTime.now());
        consumption1.setIdCreditCard("CREDIT_CARD001");
        consumption1.setDescription("Consumo en Plaza Vea");
        payment1.setCreatedAt(LocalDateTime.now());
        // Given
        when(repositoryFactory.getRepository(any())).thenReturn(creditCardRepository);
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(creditCard1));
        when(paymentService.lastTenPaymentsByIdCreditCard(idCreditCards)).thenReturn(Mono.just(List.of(payment1)));
        when(consumptionService.findLastTenConsumptions(idCreditCards)).thenReturn(Mono.just(List.of(consumption1)));
        // When
        Mono<List<Object>> tenMovements = creditCardService.getLastTenPaymentsAndConsumptionsByIdClient(idClient);
        // Then
        StepVerifier.create(tenMovements)
                .assertNext(report -> {
                    assertThat(report).isNotNull();
                    assertThat(report).hasSize(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Create a credit card with client with credit card due date")
    void createCreditCardWithDueDateInCreditCard() {

        String idClient = "clientN001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        creditCard1.setTotalDebt(1000.0);
        creditCard1.setDueDate(LocalDate.of(2025, 1, 5));
        creditCard1.setIdClient(idClient);

        CreditCard creditCard = new CreditCard();
        creditCard.setId("CREDIT_CARD002");
        creditCard.setIdClient(idClient);
        creditCard.setLimitCredit(1000.0);
        creditCard.setAvailableBalance(500.0);
        creditCard.setNumberDueDate("5");
        creditCard.setNumberBillingDate("20");
        when(repositoryFactory.getRepository(any())).thenReturn(creditCardRepository);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        when(clientRepository.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(creditCard1));

        StepVerifier.create(creditCardService.create(creditCard))
                .expectError(IneligibleClientException.class)
                .verify();
    }

    @Test
    @DisplayName("Create a credit card with client with credit due date")
    void createCreditCardWithDueDateInOnlyCredit() {

        String idClient = "clientN001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);

        credit1.setDisbursementDate(LocalDate.of(2025, 1, 15));
        credit1.setFirstDatePay(LocalDate.of(2025, 2, 5));
        credit1.setIdClient(idClient);
        credit1.setPayments(Collections.emptyList());

        CreditCard creditCard = new CreditCard();
        creditCard.setId("CREDIT_CARD002");
        creditCard.setIdClient(idClient);
        creditCard.setLimitCredit(1000.0);
        creditCard.setAvailableBalance(500.0);
        creditCard.setNumberDueDate("5");
        creditCard.setNumberBillingDate("20");
        when(repositoryFactory.getRepository(any())).thenReturn(creditCardRepository);
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        when(clientRepository.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient))
                .thenReturn(Flux.just(credit1));
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());

        StepVerifier.create(creditCardService.create(creditCard))
                .expectError(IneligibleClientException.class)
                .verify();
    }
}