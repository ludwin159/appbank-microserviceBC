package com.bank.appbank.service.impl;

import com.bank.appbank.client.MovementServiceClient;
import com.bank.appbank.dto.PaymentDto;
import com.bank.appbank.exceptions.ClientNotFoundException;
import com.bank.appbank.exceptions.IneligibleClientException;
import com.bank.appbank.exceptions.LimitAccountsException;
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.BankAccount;
import com.bank.appbank.model.Client;
import com.bank.appbank.model.Credit;
import com.bank.appbank.model.CreditCard;
import com.bank.appbank.repository.BankAccountRepository;
import com.bank.appbank.repository.CreditCardRepository;
import com.bank.appbank.repository.CreditRepository;
import com.bank.appbank.service.CreditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static com.bank.appbank.model.Client.TypeClient.PERSONAL_VIP_CLIENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class BankAccountServiceImpTest {

    @InjectMocks
    private BankAccountServiceImp bankAccountService;
    @Mock
    private RepositoryFactory repositoryFactory;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private ClientServiceImpl clientService;
    @Mock
    private CreditService creditService;
    @Mock
    private MovementServiceClient movementService;
    @Mock
    private Clock clock;

    private BankAccount bankAccount1, bankAccount2, bankAccount3;
    private Client personalClient, businessClient, holderAccount;
    private PaymentDto payment1;

    @BeforeEach
    void setUp() {
        when(repositoryFactory.getRepository(any()))
                .thenReturn(bankAccountRepository);

        List<String> authorizedSignatories = Arrays.asList("signatory1", "signatory2");
        List<String> accountHolders = List.of("idholder001");

        bankAccount1 = new BankAccount("clientN001",
                1500.0,
                BankAccount.TypeBankAccount.SAVING_ACCOUNT,
                2,
                0,
                0.0,
                20.0,
                0.5,
                5,
                authorizedSignatories,
                accountHolders);
        bankAccount1.setId("IDbank001");

        bankAccount2 = new BankAccount("clientN002",
                1200.0,
                BankAccount.TypeBankAccount.CURRENT_ACCOUNT,
                0,
                0,
                15.0,
                20.0,
                0.5,
                5,
                Collections.emptyList(),
                accountHolders);
        bankAccount2.setId("IDbank002");

        bankAccount3 = new BankAccount("clientN003",
                15.0,
                BankAccount.TypeBankAccount.FIXED_TERM_ACCOUNT,
                0,
                31,
                0.0,
                20.0,
                0.5,
                5,
                authorizedSignatories,
                accountHolders);
        bankAccount3.setId("IDbank003");

        payment1 = new PaymentDto();
        payment1.setId("PAYMENT001");
        payment1.setIdProductCredit("CREDIT001");
        payment1.setAmount(30.0);
        payment1.setMonthCorresponding(1);
        payment1.setYearCorresponding(2025);
        payment1.setTypeCreditProduct(PaymentDto.TypeCreditProduct.CREDIT);

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

        businessClient = new Client();
        businessClient.setId("clientN002");
        businessClient.setAddress("Jr. avenida Jose Pérez");
        businessClient.setTypeClient(Client.TypeClient.BUSINESS_CLIENT);
        businessClient.setEmail("business@ejemplo.com");
        businessClient.setBusinessName("Panadería Boluelos");
        businessClient.setTaxId("20756902101");

        holderAccount = new Client();
        holderAccount.setId("idholder001");
        holderAccount.setIdentity("10558954");
        holderAccount.setAddress("Jr. Puente nuevo");
        holderAccount.setTypeClient(Client.TypeClient.PERSONAL_CLIENT);
        holderAccount.setEmail("holder@ejemplo.com");
    }

    @Test
    @DisplayName("Find a BankAccount with movements")
    void findByIdTest() {
        // Given
        String idBankAccount = "IDbank001";
        when(bankAccountRepository.findById(idBankAccount)).thenReturn(Mono.just(bankAccount1));
        when(movementService.getMovementsByBankAccountIdInPresentMonth(idBankAccount)).thenReturn(Flux.empty());
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.findById(idBankAccount);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectNextMatches(element ->
                        element.getId().equals(idBankAccount) && element.getMovements().isEmpty())
                .verifyComplete();
        verify(bankAccountRepository).findById(idBankAccount);
        verify(movementService).getMovementsByBankAccountIdInPresentMonth(idBankAccount);
    }

    @Test
    @DisplayName("Create a Bank account with business client and any accounts")
    void createBusinessClientAnyAccountsTest() {
        String idBankAccount = "IDbank002";
        String idClient = "clientN002";
        // Given
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(clientService.findById(idClient)).thenReturn(Mono.just(businessClient));
        when(clientService.findAllClientsById(List.of("idholder001"))).thenReturn(Flux.just(holderAccount));
        when(bankAccountRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(Mono.just(bankAccount2));
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.create(bankAccount2);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectNextMatches(element -> element.getId().equals(idBankAccount))
                .verifyComplete();
        verify(clientService, times(1)).findById(idClient);
        verify(bankAccountRepository).save(bankAccount2);
    }

    @Test
    @DisplayName("Create a Bank account with business client and holder account not exist")
    void createBusinessClientHolderNotExistTest() {
        String idBankAccount = "IDbank002";
        String idClient = "clientN002";
        // Given
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(clientService.findById(idClient)).thenReturn(Mono.just(businessClient));
        when(clientService.findAllClientsById(List.of("idholder001"))).thenReturn(Flux.empty());
        when(bankAccountRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.create(bankAccount2);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectError(ClientNotFoundException.class)
                .verify();
        verify(clientService, times(1)).findById(idClient);
    }

    @Test
    @DisplayName("Create a Bank account with not exist client")
    void createAccountWithAnyClientTest() {
        String idBankAccount = "IDbank001";
        String idClientNotExist = "abcd123";
        bankAccount2.setIdClient(idClientNotExist);
        // Given
        when(clientService.findById(idClientNotExist)).thenReturn(Mono.error(new ResourceNotFoundException("")));
        when(bankAccountRepository.findAllByIdClient(idClientNotExist)).thenReturn(Flux.empty());
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.create(bankAccount2);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectError(ResourceNotFoundException.class)
                .verify();
        verify(clientService, times(1)).findById(idClientNotExist);
    }

    @Test
    @DisplayName("Create a Bank account with business client and several accounts and holder")
    void createBusinessClientSeveralAccountsTest() {
        String idBankAccount = "IDbank003";
        String idClient = "clientN002";
        BankAccount existingAccount = new BankAccount("clientN002",
                1200.0,
                BankAccount.TypeBankAccount.CURRENT_ACCOUNT,
                0,
                0,
                15.0,
                20.0,
                0.5,
                5,
                Collections.emptyList(),
                List.of("idholder002"));
        existingAccount.setId(idBankAccount);

        // Given
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(clientService.findById(idClient)).thenReturn(Mono.just(businessClient));
        when(clientService.findAllClientsById(List.of("idholder001"))).thenReturn(Flux.just(holderAccount));
        when(bankAccountRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(existingAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(Mono.just(bankAccount2));
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.create(bankAccount2);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectNextMatches(element -> element.getId().equals("IDbank002"))
                .verifyComplete();
        verify(clientService, times(1)).findById(idClient);
        verify(bankAccountRepository).save(bankAccount2);
    }

    @Test
    @DisplayName("Create a Bank account with business client and saving account")
    void createBusinessClientSavingAccountTest() {
        String idClient = "clientN002";
        bankAccount2.setAccountHolders(Collections.emptyList());
        // Given
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(clientService.findById(idClient)).thenReturn(Mono.just(businessClient));
        when(bankAccountRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.create(bankAccount2);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectError(IneligibleClientException.class)
                .verify();
        verify(clientService, times(1)).findById(idClient);
    }

    @Test
    @DisplayName("Create a Bank account with business client without holder account")
    void createBusinessClientWithoutHolderTest() {
        String idClient = "clientN002";
        bankAccount1.setIdClient(idClient);
        // Given
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(clientService.findById(idClient)).thenReturn(Mono.just(businessClient));
        when(bankAccountRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.create(bankAccount1);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectError(IneligibleClientException.class)
                .verify();
        verify(clientService, times(1)).findById(idClient);
    }

    @Test
    @DisplayName("Create a Bank account with personal client with a saving account already created")
    void createPersonalClientWithSavingAccountTest() {
        String idClient = "clientN001";

        BankAccount bankAccount3 = new BankAccount("clientN001",
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
        bankAccount3.setId("IDbank001");

        // Given
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(clientService.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(bankAccountRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(bankAccount1));
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.create(bankAccount3);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectError(LimitAccountsException.class)
                .verify();
        verify(clientService, times(1)).findById(idClient);
    }

    static Stream<Arguments> variableArgs() {
        return Stream.of(
                Arguments.of(Collections.emptyList(), List.of("clientN001")),
                Arguments.of(List.of("clientN001"), Collections.emptyList()),
                Arguments.of(List.of("clientN001"), List.of("clientN001"))
        );
    }
    @ParameterizedTest
    @MethodSource("variableArgs")
    @DisplayName("Create a Bank account with personal client with a saving account and holder account")
    void createPersonalClientWithSavingAccountAndHolderTest(List<String> authorizedSignatorit,
                                                             List<String> holdersAccount) {
        String idClient = "clientN001";

        BankAccount bankAccount3 = new BankAccount("clientN001",
                1500.0,
                BankAccount.TypeBankAccount.SAVING_ACCOUNT,
                2,
                0,
                0.0,
                20.0,
                0.5,
                5,
                authorizedSignatorit,
                holdersAccount);
        bankAccount3.setId("IDbank001");

        // Given
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(clientService.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(bankAccountRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(clientService.findAllClientsById(List.of("clientN001"))).thenReturn(Flux.just(personalClient));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(Mono.just(bankAccount3));
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.create(bankAccount3);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectNextMatches(element -> element.getId().equals(bankAccount3.getId()))
                .verifyComplete();
        verify(clientService, times(1)).findById(idClient);
        verify(bankAccountRepository).save(bankAccount3);
    }
    @Test
    @DisplayName("Create a Bank account with personal client with a current account already created")
    void createPersonalClientWithCurrentAccountCreated() {
        String idClient = "clientN001";

        BankAccount bankAccount3 = new BankAccount("clientN001",
                1500.0,
                BankAccount.TypeBankAccount.CURRENT_ACCOUNT,
                2,
                0,
                0.0,
                20.0,
                0.5,
                5,
                Collections.emptyList(),
                Collections.emptyList());
        bankAccount3.setId("IDbank001");

        bankAccount2.setIdClient(idClient);
        // Given
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(clientService.findById(idClient)).thenReturn(Mono.just(personalClient));
        when(bankAccountRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(bankAccount2));
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.create(bankAccount3);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectError(LimitAccountsException.class)
                .verify();
        verify(clientService, times(1)).findById(idClient);
    }

    @Test
    @DisplayName("Update a BankAccount Simple")
    void updateBankAccountSimpleTest() {
        // Given
        String idBankAccount = "clientN001";
        bankAccount1.setLimitMovements(5);
        bankAccount1.setAuthorizedSignatorits(Collections.emptyList());
        when(bankAccountRepository.findById(idBankAccount)).thenReturn(Mono.just(bankAccount1));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(Mono.just(bankAccount1));
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.update(idBankAccount, bankAccount1);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectNextMatches(element -> element.getLimitMovements() == 5 &&
                        element.getAuthorizedSignatorits().isEmpty())
                .verifyComplete();
        verify(bankAccountRepository).findById(idBankAccount);
        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Update a BankAccount with change client id and same type client")
    void updateClientIdTest() {
        String idBankAccount = "IDbank001";
        BankAccount newBankAccount = new BankAccount("idholder001",
                1500.0,
                BankAccount.TypeBankAccount.SAVING_ACCOUNT,
                2,
                0,
                0.0,
                20.0,
                0.5,
                5,
                bankAccount1.getAuthorizedSignatorits(),
                bankAccount1.getAccountHolders());
        newBankAccount.setId("IDbank001");

        // Given
        when(bankAccountRepository.findById(idBankAccount)).thenReturn(Mono.just(bankAccount1));
        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(Mono.just(bankAccount1));
        when(clientService.findById("idholder001")).thenReturn(Mono.just(holderAccount));
        when(clientService.findById("clientN001")).thenReturn(Mono.just(personalClient));
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.update(idBankAccount, newBankAccount);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectNextMatches(element -> element.getIdClient().equals("idholder001"))
                .verifyComplete();
        verify(bankAccountRepository).findById(idBankAccount);
        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Update a BankAccount with change client id and different type client")
    void updateClientIdDifferentTest() {
        String idBankAccount = "IDbank001";
        BankAccount newBankAccount = new BankAccount("clientN002",
                1500.0,
                BankAccount.TypeBankAccount.SAVING_ACCOUNT,
                2,
                0,
                0.0,
                20.0,
                0.5,
                5,
                bankAccount1.getAuthorizedSignatorits(),
                bankAccount1.getAccountHolders());
        newBankAccount.setId("IDbank001");

        // Given
        when(bankAccountRepository.findById(idBankAccount)).thenReturn(Mono.just(bankAccount1));
        when(clientService.findById("clientN002")).thenReturn(Mono.just(businessClient));
        when(clientService.findById("clientN001")).thenReturn(Mono.just(personalClient));
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.update(idBankAccount, newBankAccount);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectError(IneligibleClientException.class)
                .verify();
        verify(bankAccountRepository).findById(idBankAccount);
    }

    @Test
    @DisplayName("Update a BankAccount with change client id and not found client")
    void updateClientNotFoundTest() {
        String idBankAccount = "IDbank001";
        BankAccount newBankAccount = new BankAccount("clientxxxx",
                1500.0,
                BankAccount.TypeBankAccount.SAVING_ACCOUNT,
                2,
                0,
                0.0,
                20.0,
                0.5,
                5,
                bankAccount1.getAuthorizedSignatorits(),
                bankAccount1.getAccountHolders());
        newBankAccount.setId("IDbank001");

        // Given
        when(bankAccountRepository.findById(idBankAccount)).thenReturn(Mono.just(bankAccount1));
        when(clientService.findById("clientxxxx")).thenReturn(Mono.error(new ResourceNotFoundException("")));
        when(clientService.findById("clientN001")).thenReturn(Mono.just(personalClient));
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.update(idBankAccount, newBankAccount);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectError(ClientNotFoundException.class)
                .verify();
        verify(bankAccountRepository).findById(idBankAccount);
    }

    @Test
    @DisplayName("Find bank accounts with all movements")
    void findBankAccountsByIdClientWithAllMovementsSortedByDateTest() {
        // Given
        String idClient = "clientN001";
        when(bankAccountRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(bankAccount1));
        when(movementService.getAllMovementsByIdBankAccountAndSortByDate(bankAccount1.getId()))
                .thenReturn(Flux.empty());
        // When
        Flux<BankAccount> bankAccountFlux = bankAccountService
                .findBankAccountsByIdClientWithAllMovementsSortedByDate(idClient);
        // Then
        StepVerifier.create(bankAccountFlux)
                .expectNextMatches(element -> element.getId().equals(bankAccount1.getId())
                        && element.getMovements().isEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("Create saving account with personal vip client")
    void createVipClientTest() {
        String idClient = "clientN001";
        CreditCard creditCard1 = new CreditCard();
        creditCard1.setId("CREDIT_CARD001");
        creditCard1.setIdClient("clientN001");
        creditCard1.setLimitCredit(1000.0);
        creditCard1.setAvailableBalance(500.0);
        personalClient.setTypeClient(PERSONAL_VIP_CLIENT);
        bankAccount1.setIdClient(personalClient.getId());
        // Given
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(clientService.findById(personalClient.getId())).thenReturn(Mono.just(personalClient));
        when(creditCardRepository.findAllByIdClient(personalClient.getId())).thenReturn(Flux.just(creditCard1));
        when(bankAccountRepository.findAllByIdClient(personalClient.getId()))
                .thenReturn(Flux.empty());
        when(bankAccountRepository.save(bankAccount1)).thenReturn(Mono.just(bankAccount1));
        // When

        Mono<BankAccount> bankAccountMono = bankAccountService.create(bankAccount1);
        // Then
        StepVerifier.create(bankAccountMono)
                .assertNext(bankAccount -> {
                    assertThat(bankAccount).isNotNull();
                    assertThat(bankAccount.getTypeBankAccount()).isEqualTo(BankAccount.TypeBankAccount.SAVING_ACCOUNT);
                    assertThat(bankAccount.getId()).isEqualTo(bankAccount1.getId());
                })
                .verifyComplete();
    }
    @Test
    @DisplayName("Create saving account with personal vip without credit card")
    void createClientVipWithoutCreditCardTest() {
        String idClient = "clientN001";
        CreditCard creditCard1 = new CreditCard();
        creditCard1.setId("CREDIT_CARD001");
        creditCard1.setIdClient("clientN001");
        creditCard1.setLimitCredit(1000.0);
        creditCard1.setAvailableBalance(500.0);
        personalClient.setTypeClient(PERSONAL_VIP_CLIENT);
        bankAccount1.setIdClient(personalClient.getId());
        // Given
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.empty());
        when(clientService.findById(personalClient.getId())).thenReturn(Mono.just(personalClient));
        when(creditCardRepository.findAllByIdClient(personalClient.getId())).thenReturn(Flux.empty());
        when(bankAccountRepository.findAllByIdClient(personalClient.getId()))
                .thenReturn(Flux.empty());

        Mono<BankAccount> bankAccountMono = bankAccountService.create(bankAccount1);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectError(IneligibleClientException.class)
                .verify();
    }

    @Test
    @DisplayName("Create bank account with due date in credit card")
    void createBankAccountWithDueDateTest() {
        String idClient = "clientN001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        CreditCard creditCard1 = new CreditCard();
        creditCard1.setId("CREDIT_CARD001");
        creditCard1.setIdClient("clientN001");
        creditCard1.setLimitCredit(1000.0);
        creditCard1.setAvailableBalance(500.0);
        personalClient.setTypeClient(PERSONAL_VIP_CLIENT);
        bankAccount1.setIdClient(personalClient.getId());
        creditCard1.setTotalDebt(1000.0);
        creditCard1.setDueDate(LocalDate.of(2025, 1, 5));
        // Given
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient)).thenReturn(Flux.empty());
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(creditCard1));
        when(clientService.findById(personalClient.getId())).thenReturn(Mono.just(personalClient));
        when(bankAccountRepository.findAllByIdClient(personalClient.getId()))
                .thenReturn(Flux.empty());

        Mono<BankAccount> bankAccountMono = bankAccountService.create(bankAccount1);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectError(IneligibleClientException.class)
                .verify();
    }

    @Test
    @DisplayName("Create bank account with due date in credit")
    void createBankAccountWithDueDateCreditTest() {
        String idClient = "clientN001";
        ZoneId zone = ZoneId.of("UTC");
        Instant instant = Instant.parse("2025-02-20T23:55:00Z");
        Clock fixedClock = Clock.fixed(instant, zone);
        CreditCard creditCard1 = new CreditCard();
        creditCard1.setId("CREDIT_CARD001");
        creditCard1.setIdClient("clientN001");
        creditCard1.setLimitCredit(1000.0);
        creditCard1.setAvailableBalance(500.0);
        personalClient.setTypeClient(PERSONAL_VIP_CLIENT);
        bankAccount1.setIdClient(personalClient.getId());
        creditCard1.setTotalDebt(1000.0);
        creditCard1.setDueDate(LocalDate.of(2025, 1, 5));
        payment1 = new PaymentDto();
        payment1.setMonthCorresponding(12);
        payment1.setYearCorresponding(2024);
        payment1.setTypeCreditProduct(PaymentDto.TypeCreditProduct.CREDIT);

        Credit credit1 = new Credit(
                "clientN001",
                500.0,
                200.0,
                0.15,
                LocalDate.of(2024, 11, 5),
                LocalDate.of(2024, 12, 2),
                12,
                0.0
        );
        credit1.setId("CREDIT001");
        // Given
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
        when(creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient))
                .thenReturn(Flux.just(credit1));
        when(creditCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(creditCard1));
        when(clientService.findById(personalClient.getId())).thenReturn(Mono.just(personalClient));
        when(bankAccountRepository.findAllByIdClient(personalClient.getId()))
                .thenReturn(Flux.empty());

        Mono<BankAccount> bankAccountMono = bankAccountService.create(bankAccount1);
        // Then
        StepVerifier.create(bankAccountMono)
                .expectError(IneligibleClientException.class)
                .verify();
    }

    @Test
    @DisplayName("Find by id bank account without movements")
    public void findByIdWithoutMovementsTest() {
        // Given
        when(bankAccountRepository.findById(bankAccount1.getId())).thenReturn(Mono.just(bankAccount1));
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.findByIdWithoutMovements(bankAccount1.getId());
        // Then
        StepVerifier.create(bankAccountMono)
                .assertNext(bankAccount -> {
                    assertThat(bankAccount.getMovements().size()).isEqualTo(0);
                })
                .verifyComplete();
        verify(bankAccountRepository).findById(bankAccount1.getId());
    }

    @Test
    @DisplayName("Find by id bank account without movements and error")
    public void findByIdWithoutMovementsErrorTest() {
        // Given
        when(bankAccountRepository.findById(bankAccount1.getId())).thenReturn(Mono.empty());
        // When
        Mono<BankAccount> bankAccountMono = bankAccountService.findByIdWithoutMovements(bankAccount1.getId());
        // Then
        StepVerifier.create(bankAccountMono)
                .expectError(ResourceNotFoundException.class)
                .verify();
        verify(bankAccountRepository).findById(bankAccount1.getId());
    }

}