package com.bank.appbank.service.impl;

import com.bank.appbank.client.MovementServiceClient;
import com.bank.appbank.dto.MovementDto;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.BankAccount;
import com.bank.appbank.model.BankAccountDebitCard;
import com.bank.appbank.model.DebitCard;
import com.bank.appbank.repository.BankAccountDebitCardRepository;
import com.bank.appbank.repository.BankAccountRepository;
import com.bank.appbank.repository.DebitCardRepository;
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

import java.time.LocalDateTime;
import java.util.Collections;
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
}