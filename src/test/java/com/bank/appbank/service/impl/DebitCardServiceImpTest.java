package com.bank.appbank.service.impl;

import com.bank.appbank.client.MovementServiceClient;
import com.bank.appbank.dto.MovementDto;
import com.bank.appbank.dto.ResponseAssociationWalletDto;
import com.bank.appbank.event.producer.DebitCardProducer;
import com.bank.appbank.exceptions.BadInformationException;
import com.bank.appbank.exceptions.IneligibleClientException;
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.*;
import com.bank.appbank.repository.*;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class DebitCardServiceImpTest {

    @InjectMocks
    private DebitCardServiceImp debitCardService;
    @Mock
    private BankAccountDebitCardRepository bankAccountDebitCardRepository;
    @Mock
    private MovementServiceClient movementServiceClient;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private RepositoryFactory repositoryFactory;
    @Mock
    private DebitCardRepository debitCardRepository;
    @Mock
    private MovementWalletRepository movementWalletRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private DebitCardProducer debitCardProducer;
    private DebitCard debitCard1;
    private BankAccount bankAccount1;
    private BankAccountDebitCard bankAccountDebitCard;
    private MovementDto movement1;

    @BeforeEach
    void setUp() {
        bankAccount1 = new BankAccount("clientN001",
                1500.0,
                BankAccount.TypeBankAccount.SAVING_ACCOUNT,
                2,
                0,
                0.0,
                0.2,
                0.5,
                5,
                Collections.emptyList(),
                Collections.emptyList());
        bankAccount1.setId("IDbank001");

        when(repositoryFactory.getRepository(any())).thenReturn(debitCardRepository);
        debitCard1 = new DebitCard();
        debitCard1.setId("DEBITCARD001");
        debitCard1.setIdClient("clientN001");
        debitCard1.setIdPrincipalAccount(bankAccount1.getId());

        bankAccountDebitCard = new BankAccountDebitCard();
        bankAccountDebitCard.setIdDebitCard(debitCard1.getId());
        bankAccountDebitCard.setId("BANKACCOUNTDEBIT001");
        bankAccountDebitCard.setCreatedAt(LocalDateTime.now());
        bankAccountDebitCard.setIdBankAccount(bankAccount1.getId());

        movement1 = new MovementDto();
        movement1.setId("IDMOVEMENT01");
        movement1.setIdBankAccount("BANKACCOUNTDEBIT001");
        movement1.setAmount(20.0);
        movement1.setCommissionAmount(5.0);
        movement1.setTypeMovement(MovementDto.TypeMovement.DEPOSIT);
        movement1.setDescription("Deposit Movement");
        movement1.setIdBankAccountTransfer("");
        movement1.setIdTransfer("");

    }

    @Test
    @DisplayName("Get last 10 movement by debit card")
    void getLastTenMovementsDebitCard() {
        String idClient = "CLIENT001";
        debitCard1.setIdClient(idClient);

        MovementDto movement2 = new MovementDto();
        movement2.setId("IDMOVEMENT02");
        movement2.setIdBankAccount(bankAccount1.getId());
        movement2.setAmount(10.0);
        movement2.setCommissionAmount(2.0);
        movement2.setTypeMovement(MovementDto.TypeMovement.WITHDRAWAL);
        movement2.setDescription("Withdrawal Movement");
        movement2.setIdBankAccountTransfer("");
        movement2.setIdTransfer("");
        // Given

        List<String> idsDebitCards = List.of(debitCard1.getId());
        List<String> idsAccounts = List.of(bankAccount1.getId());
        List<MovementDto> movements = List.of(movement1, movement2);
        when(debitCardRepository.findAllByIdClient(idClient)).thenReturn(Flux.just(debitCard1));
        when(bankAccountDebitCardRepository.findAllByIdDebitCardIn(idsDebitCards))
                .thenReturn(Flux.just(bankAccountDebitCard));
        when(movementServiceClient.getLastTenMovements(idsAccounts)).thenReturn(Mono.just(movements));
        // When
        Mono<List<MovementDto>> movementsResponse = debitCardService.getLastTenMovementsDebitCard(idClient);
        // Then
        StepVerifier.create(movementsResponse)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertThat(response.size()).isEqualTo(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Get balance in principal bank account")
    void getBalanceOfBankAccountInDebitCard() {
        String idDebitCard = debitCard1.getId();
        // Given
        when(debitCardRepository.findById(idDebitCard)).thenReturn(Mono.just(debitCard1));
        when(bankAccountRepository.findById(debitCard1.getIdPrincipalAccount())).thenReturn(Mono.just(bankAccount1));
        // When
        Mono<Map<String, Object>> responseMono = debitCardService.getBalanceOfBankAccountInDebitCard(idDebitCard);
        // then
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertThat(response.get("balancePrincipalBankAccount")).isEqualTo(bankAccount1.getBalance());
                    assertThat(response.get("idBankAccount")).isEqualTo(bankAccount1.getId());
                })
                .verifyComplete();
        verify(debitCardRepository).findById(idDebitCard);
        verify(bankAccountRepository).findById(debitCard1.getIdPrincipalAccount());
    }

    @Test
    @DisplayName("Add an Bank account to debit card")
    void addBankAccountToDebitCardTest() {
        String idBankAccount = bankAccount1.getId();
        // Given
        when(bankAccountRepository.findById(idBankAccount)).thenReturn(Mono.just(bankAccount1));
        when(debitCardRepository.findById(debitCard1.getId())).thenReturn(Mono.just(debitCard1));
        when(bankAccountDebitCardRepository.findAllByIdDebitCard(debitCard1.getId()))
                .thenReturn(Flux.empty());
        when(bankAccountDebitCardRepository.save(bankAccountDebitCard)).thenReturn(Mono.just(bankAccountDebitCard));
        // When
        Mono<BankAccountDebitCard> bankAccountDebitCardMono = debitCardService
                .addBankAccountToDebitCard(bankAccountDebitCard);
        // Then
        StepVerifier.create(bankAccountDebitCardMono)
                .assertNext(itemSaved -> {
                    assertThat(itemSaved.getId()).isEqualTo(bankAccountDebitCard.getId());
                    assertThat(itemSaved.getIdBankAccount()).isEqualTo(bankAccountDebitCard.getIdBankAccount());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Add an Bank account to debit card when already exists bank account")
    void addBankAccountToDebitCardAlreadyExistTest() {
        String idBankAccount = bankAccount1.getId();
        BankAccount bankAccount = new BankAccount("clientN001",
                1500.0,
                BankAccount.TypeBankAccount.CURRENT_ACCOUNT,
                2,
                0,
                0.0,
                0.2,
                0.5,
                5,
                Collections.emptyList(),
                Collections.emptyList());
        bankAccount.setId("IDbank002");

        BankAccountDebitCard bankAccountDebitCard1 = new BankAccountDebitCard();
        bankAccountDebitCard1.setIdDebitCard(debitCard1.getId());
        bankAccountDebitCard1.setId("BANKACCOUNTDEBIT002");
        bankAccountDebitCard1.setCreatedAt(LocalDateTime.now());
        bankAccountDebitCard1.setIdBankAccount(bankAccount.getId());
        // Given
        when(bankAccountRepository.findById(idBankAccount)).thenReturn(Mono.just(bankAccount1));
        when(debitCardRepository.findById(debitCard1.getId())).thenReturn(Mono.just(debitCard1));
        when(bankAccountDebitCardRepository.findAllByIdDebitCard(debitCard1.getId()))
                .thenReturn(Flux.just(bankAccountDebitCard, bankAccountDebitCard1));
        // When
        Mono<BankAccountDebitCard> bankAccountDebitCardMono =
                debitCardService.addBankAccountToDebitCard(bankAccountDebitCard);
        // Then
        StepVerifier.create(bankAccountDebitCardMono)
                .expectError(BadInformationException.class)
                .verify();
    }

    @Test
    @DisplayName("Add an Bank account to debit card with different client")
    void addBankAccountToDebitCardDifferentClientTest() {
        String idBankAccount = bankAccount1.getId();
        // Given
        debitCard1.setIdClient("OTROCLIENT");
        when(bankAccountRepository.findById(idBankAccount)).thenReturn(Mono.just(bankAccount1));
        when(debitCardRepository.findById(debitCard1.getId())).thenReturn(Mono.just(debitCard1));
        // When
        Mono<BankAccountDebitCard> assign = debitCardService.addBankAccountToDebitCard(bankAccountDebitCard);
        // Then
        StepVerifier.create(assign)
                .expectError(IneligibleClientException.class)
                .verify();
    }

    @Test
    @DisplayName("Find debit card by id and bank accounts order by date creation")
    void findByIdWithBankAccountsOrderByCreatedAtTest() {
        String idDebitCard = debitCard1.getId();
        BankAccount bankAccount = new BankAccount("clientN001",
                1500.0,
                BankAccount.TypeBankAccount.CURRENT_ACCOUNT,
                2,
                0,
                0.0,
                0.2,
                0.5,
                5,
                Collections.emptyList(),
                Collections.emptyList());
        bankAccount.setId("IDbank002");

        BankAccountDebitCard bankAccountDebitCard1 = new BankAccountDebitCard();
        bankAccountDebitCard1.setIdDebitCard(debitCard1.getId());
        bankAccountDebitCard1.setId("BANKACCOUNTDEBIT002");
        bankAccountDebitCard1.setCreatedAt(LocalDateTime.now());
        bankAccountDebitCard1.setIdBankAccount(bankAccount.getId());
        List<String> ids = List.of(bankAccount.getId(), bankAccount1.getId());
        // Given
        when(debitCardRepository.findById(idDebitCard)).thenReturn(Mono.just(debitCard1));
        when(bankAccountDebitCardRepository.findAllByIdDebitCardOrderByCreatedAtAsc(idDebitCard))
                .thenReturn(Flux.just(bankAccountDebitCard1, bankAccountDebitCard));
        when(bankAccountRepository.findByIdIn(ids)).thenReturn(Flux.just(bankAccount, bankAccount1));
        // When
        Mono<DebitCard> debitCardMono = debitCardService.findByIdWithBankAccountsOrderByCreatedAt(idDebitCard);
        // Then
        StepVerifier.create(debitCardMono)
                .assertNext(debitCard -> {
                    assertThat(debitCard.getIdClient()).isEqualTo("clientN001");
                    assertThat(debitCard.getBankAccounts().size()).isEqualTo(2);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Validate association debit card with wallet yanki")
    void validAssociationWalletToDebitCardTest() {
        String idWallet = "WALLET_01";
        debitCard1.setHasWallet(false);
        ResponseAssociationWalletDto responseValid = ResponseAssociationWalletDto.builder()
                .idWallet(idWallet)
                .idDebitCard(debitCard1.getId())
                .state("APPROVE")
                .balance(bankAccount1.getBalance())
                .observation("Association Approved").build();
        // Given
        when(debitCardRepository.findById(debitCard1.getId())).thenReturn(Mono.just(debitCard1));
        when(bankAccountRepository.findById(debitCard1.getIdPrincipalAccount())).thenReturn(Mono.just(bankAccount1));
        when(debitCardRepository.save(debitCard1)).thenReturn(Mono.just(debitCard1));
        // When
        Mono<ResponseAssociationWalletDto> responseAssociation =
                debitCardService.validAssociationWalletToDebitCard(idWallet, debitCard1.getId());
        // Then
        StepVerifier.create(responseAssociation)
                .assertNext(response -> {
                    assertThat(response.getIdDebitCard()).isEqualTo(responseValid.getIdDebitCard());
                    assertThat(response.getState()).isEqualTo(responseValid.getState());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Validate payment with wallet with debit card")
    void paymentWalletWithDebitCardTest() {
        MovementWallet movementWallet1 = MovementWallet.builder()
                .id("WALLET_1")
                .amount(20.0)
                .type(MovementWallet.TypeMovementWallet.MAKE_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.PENDING)
                .description("Test payment")
                .numberDestin("900000000")
                .createdAt(Date.from(Instant.now()))
                .idDebitCard(debitCard1.getId()).build();
        MovementWallet movementWallet2 = MovementWallet.builder()
                .id("WALLET_2")
                .amount(20.0)
                .type(MovementWallet.TypeMovementWallet.RECEIPT_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.PENDING)
                .description("Test payment")
                .numberDestin("900000000")
                .createdAt(Date.from(Instant.now()))
                .idDebitCard("").build();

        List<MovementWallet> movementsWallet = List.of(movementWallet1, movementWallet2);
        List<String> idsDebitCards = List.of(movementWallet1.getIdDebitCard(), movementWallet2.getIdDebitCard());
        List<String> idsBankAccounts = List.of(debitCard1.getIdPrincipalAccount());
        // Given
        when(debitCardRepository.findAllByIdIn(idsDebitCards)).thenReturn(Flux.just(debitCard1));
        when(movementWalletRepository.save(movementWallet1)).thenReturn(Mono.just(movementWallet1));
        when(movementWalletRepository.save(movementWallet2)).thenReturn(Mono.just(movementWallet2));
        when(bankAccountRepository.findByIdIn(idsBankAccounts)).thenReturn(Flux.just(bankAccount1));
        when(bankAccountRepository.save(bankAccount1)).thenReturn(Mono.just(bankAccount1));
        doNothing().when(debitCardProducer).publishConfirmationPaymentDebit(movementsWallet);
        // When
        Mono<Void> doPayment = debitCardService.paymentWalletWithDebitCard(movementsWallet);
        // Then
        StepVerifier.create(doPayment)
                .verifyComplete();

    }
    @Test
    @DisplayName("Validate payment with error payment")
    void paymentWalletWithErrorTest() {
        MovementWallet movementWallet1 = MovementWallet.builder()
                .id("WALLET_1")
                .amount(20.0)
                .type(MovementWallet.TypeMovementWallet.MAKE_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.PENDING)
                .description("Test payment")
                .numberDestin("900000000")
                .createdAt(Date.from(Instant.now()))
                .idDebitCard(debitCard1.getId()).build();
        MovementWallet movementWallet2 = MovementWallet.builder()
                .id("WALLET_2")
                .amount(20.0)
                .type(MovementWallet.TypeMovementWallet.RECEIPT_PAYMENT)
                .stateMovement(MovementWallet.StateMovement.PENDING)
                .description("Test payment")
                .numberDestin("900000000")
                .createdAt(Date.from(Instant.now()))
                .idDebitCard("").build();
        bankAccount1.setBalance(19.0);
        List<MovementWallet> movementsWallet = List.of(movementWallet1, movementWallet2);
        List<String> idsDebitCards = List.of(movementWallet1.getIdDebitCard(), movementWallet2.getIdDebitCard());
        List<String> idsPrincipalBankAccount = List.of(bankAccount1.getId());
        // Given
        when(debitCardRepository.findAllByIdIn(idsDebitCards)).thenReturn(Flux.just(debitCard1));
        when(bankAccountRepository.findByIdIn(idsPrincipalBankAccount)).thenReturn(Flux.just(bankAccount1));
        when(movementWalletRepository.save(movementWallet1)).thenReturn(Mono.just(movementWallet1));
        when(movementWalletRepository.save(movementWallet2)).thenReturn(Mono.just(movementWallet2));

        // When
        Mono<Void> doPayment = debitCardService.paymentWalletWithDebitCard(movementsWallet);
        // Then
        StepVerifier.create(doPayment)
                .verifyComplete();

    }

    @Test
    @DisplayName("Create a debit card test")
    void createDebitCardTest() {
        Client personalClient = new Client();
        personalClient.setId("clientN001");
        personalClient.setAddress("Jr. avenida");
        personalClient.setTypeClient(Client.TypeClient.PERSONAL_CLIENT);
        personalClient.setEmail("ejemplo@ejemplo.com");
        personalClient.setFullName("Lucas Juan");
        personalClient.setBusinessName("");
        personalClient.setPhone("");
        personalClient.setTaxId("");
        personalClient.setIdentity("75690210");
        BankAccountDebitCard register = new BankAccountDebitCard();
        register.setId("REGISTER_1");
        register.setCreatedAt(LocalDateTime.now());
        register.setIdBankAccount(bankAccount1.getId());
        register.setIdDebitCard(debitCard1.getId());
        // Given
        debitCard1.setIdPrincipalAccount(bankAccount1.getId());
        when(clientRepository.findById(debitCard1.getIdClient())).thenReturn(Mono.just(personalClient));
        when(bankAccountRepository.findById(debitCard1.getIdPrincipalAccount())).thenReturn(Mono.just(bankAccount1));
        when(debitCardRepository.save(debitCard1)).thenReturn(Mono.just(debitCard1));
        when(bankAccountDebitCardRepository.save(any(BankAccountDebitCard.class))).thenReturn(Mono.just(register));
        // When
        Mono<DebitCard> debitCardMono = debitCardService.create(debitCard1);
        // Then
        StepVerifier.create(debitCardMono)
                .assertNext(debitCard -> {
                    assertNotNull(debitCard);
                    assertThat(debitCard.getId()).isEqualTo(debitCard1.getId());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Update an debit card with correct information")
    void updateDebitCardOnlyNormalField() {
        DebitCard debitCard = new DebitCard();
        debitCard.setId("DEBITCARD001");
        debitCard.setIdClient("clientN001");
        debitCard.setNumberCard("NEW-NUMBER-001");
        debitCard.setIdPrincipalAccount(bankAccount1.getId());
        // Given
        when(debitCardRepository.findById(debitCard.getId())).thenReturn(Mono.just(debitCard1));
        when(debitCardRepository.save(any(DebitCard.class))).thenReturn(Mono.just(debitCard1));
        // When // Then
        StepVerifier.create(debitCardService.update(debitCard.getId(), debitCard))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertThat(response.getId()).isEqualTo(debitCard.getId());
                    assertThat(response.getNumberCard()).isEqualTo(debitCard.getNumberCard());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Update an debit card when not exist")
    void updateDebitCardNotExist() {
        String idNotExist = "IDNOTEXIT";
        // Given
        when(debitCardRepository.findById(idNotExist)).thenReturn(Mono.empty());
        // When, Then
        StepVerifier.create(debitCardService.update(idNotExist, debitCard1))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Update an debit card with different client")
    void updateDebitCardWithDifferentClient() {
        DebitCard debitCard = new DebitCard();
        debitCard.setId("DEBITCARD001");
        debitCard.setIdClient("clientN002");
        debitCard.setIdPrincipalAccount(bankAccount1.getId());
        // Given
        when(debitCardRepository.findById(debitCard1.getId())).thenReturn(Mono.just(debitCard1));
        // When, Then
        StepVerifier.create(debitCardService.update(debitCard.getId(), debitCard))
                .expectError(IneligibleClientException.class)
                .verify();
    }

    @Test
    @DisplayName("Update a debit card with different principal bank account")
    void updateDebitCardWithDifferentAccountTest() {
        BankAccount bankAccount2 = new BankAccount("clientN002",
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

        BankAccountDebitCard bankAccountDebitCard1 = new BankAccountDebitCard();
        bankAccountDebitCard1.setIdDebitCard(debitCard1.getId());
        bankAccountDebitCard1.setId("BANKACCOUNTDEBIT002");
        bankAccountDebitCard1.setCreatedAt(LocalDateTime.now());
        bankAccountDebitCard1.setIdBankAccount(bankAccount2.getId());

        DebitCard debitCard = new DebitCard();
        debitCard.setId("DEBITCARD001");
        debitCard.setIdClient("clientN001");
        debitCard.setIdPrincipalAccount(bankAccount2.getId());

        List<String> idsBanks = List.of(bankAccount2.getId());
        // Given
        when(debitCardRepository.findById(debitCard1.getId())).thenReturn(Mono.just(debitCard1));
        when(bankAccountDebitCardRepository.findAllByIdDebitCard(debitCard1.getId()))
                .thenReturn(Flux.just(bankAccountDebitCard1));
        when(debitCardRepository.save(any(DebitCard.class))).thenReturn(Mono.just(debitCard));
        // When
        Mono<DebitCard> debitCardMono = debitCardService.update(debitCard1.getId(), debitCard);
        // Then
        StepVerifier.create(debitCardMono)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertThat(response.getIdPrincipalAccount()).isEqualTo(debitCard.getIdPrincipalAccount());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Delete debit card")
    void deleteDebitCardTest() {
        // Given
        when(debitCardRepository.findById(debitCard1.getId())).thenReturn(Mono.just(debitCard1));
        when(debitCardRepository.deleteById(debitCard1.getId())).thenReturn(Mono.empty());
        // When// Then
        StepVerifier.create(debitCardService.deleteDebitCard(debitCard1.getId()))
                .verifyComplete();
    }

    @Test
    @DisplayName("Delete debit card when not exits")
    void deleteDebitCardNotFoundTest() {
        // Given
        when(debitCardRepository.findById(debitCard1.getId())).thenReturn(Mono.empty());
        // When// Then
        StepVerifier.create(debitCardService.deleteDebitCard(debitCard1.getId()))
                .expectError()
                .verify();
    }
}