package com.bank.appbank.service.impl;

import com.bank.appbank.client.PaymentServiceClient;
import com.bank.appbank.exceptions.CanNotDeleteEntity;
import com.bank.appbank.exceptions.CreditInvalid;
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.Client;
import com.bank.appbank.model.Credit;
import com.bank.appbank.dto.PaymentDto;
import com.bank.appbank.model.CreditCard;
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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditServiceImpTest {

    @InjectMocks
    private CreditServiceImp creditService;

    @Mock
    private ClientService clientService;
    @Mock
    private PaymentServiceClient paymentService;
    @Mock
    private RepositoryFactory repositoryFactory;
    @Mock
    private CreditRepository creditRepository;

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
                15.0
        );
        credit1.setId("CREDIT001");

        creditCard1 = new CreditCard();
        creditCard1.setId("CREDIT_CARD001");
        creditCard1.setIdClient("clientN001");
        creditCard1.setLimitCredit(1000.0);
        creditCard1.setAvailableBalance(500.0);
        creditCard1.setInterestRate(3.0);

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
        when(paymentService
                .findAllPaymentByIdProductCreditAndSortByDate(idCredit))
                .thenReturn(Flux.just(payment1, payment2));
        // WHen
        Mono<Credit> creditMono = creditService.findById(idCredit);
        // Then
        StepVerifier.create(creditMono)
                .expectNextMatches(credit -> credit.getId().equals(idCredit) && credit.getPayments().size() == 2)
                .verifyComplete();
        verify(creditRepository).findById(idCredit);
        verify(paymentService).findAllPaymentByIdProductCreditAndSortByDate(idCredit);
    }

    @Test
    @DisplayName("update a credit")
    void updateCreditTest() {
        String idCredit = "CREDIT001";
        Credit creditNew = new Credit(
                "clientN001",
                500.0,
                100.0,
                5.0
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
                1.0
        );
        credit2.setId("CREDIT002");
        // Given
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(credit1, credit2));
        when(paymentService.findAllPaymentByIdProductCreditAndSortByDate(credit1.getId()))
                .thenReturn(Flux.just(payment1, payment2));
        when(paymentService.findAllPaymentByIdProductCreditAndSortByDate(credit2.getId()))
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
        verify(paymentService).findAllPaymentByIdProductCreditAndSortByDate(credit1.getId());
    }

    @Test
    @DisplayName("Create a credit personal with already credit")
    void createCreditTest() {
        String idClient = "clientN001";
        Credit existCredit = new Credit(
                "clientN001",
                2000.0,
                1800.0,
                15.0
        );
        existCredit.setId("CREDIT002");
        // Given
        when(clientService.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(existCredit));
        // When
        Mono<Credit> creditMono = creditService.create(credit1);
        // Then
        StepVerifier.create(creditMono)
                .expectError(CreditInvalid.class)
                .verify();
        verify(clientService).findById(idClient);
        verify(creditRepository).findAllByIdClient(idClient);
    }

    @Test
    @DisplayName("Create a credit personal without credits")
    void createCreditWithoutCreditsTest() {
        String idClient = "clientN001";
        // Given
        when(clientService.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(creditRepository.save(credit1)).thenReturn(Mono.just(credit1));
        // When
        Mono<Credit> creditMono = creditService.create(credit1);
        // Then
        StepVerifier.create(creditMono)
                .expectNextMatches(credit -> credit.getId().equals(credit1.getId()))
                .verifyComplete();
        verify(clientService).findById(idClient);
        verify(creditRepository).findAllByIdClient(idClient);
        verify(creditRepository).save(credit1);
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
        credit1.setIdClient("clientN002");
        // Given
        when(clientService.findById(idClient)).thenReturn(Mono.just(businessClient));
        when(creditRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(credit1));
        when(creditRepository.save(credit1)).thenReturn(Mono.just(credit1));
        // When
        Mono<Credit> creditMono = creditService.create(credit1);
        // Then
        StepVerifier.create(creditMono)
                .expectNextMatches(credit -> credit.getId().equals(credit1.getId()))
                .verifyComplete();
        verify(clientService).findById(idClient);
        verify(creditRepository).findAllByIdClient(idClient);
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

}