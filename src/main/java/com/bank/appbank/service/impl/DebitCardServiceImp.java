package com.bank.appbank.service.impl;

import com.bank.appbank.client.MovementServiceClient;
import com.bank.appbank.dto.MovementDto;
import com.bank.appbank.dto.PaymentDto;
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.BankAccountDebitCard;
import com.bank.appbank.model.DebitCard;
import com.bank.appbank.repository.BankAccountDebitCardRepository;
import com.bank.appbank.repository.BankAccountRepository;
import com.bank.appbank.repository.DebitCardRepository;
import com.bank.appbank.service.DebitCardService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DebitCardServiceImp extends ServiceGenImp<DebitCard, String> implements DebitCardService {
    private final BankAccountDebitCardRepository bankAccountDebitCardRepository;
    private final MovementServiceClient movementServiceClient;
    private final BankAccountRepository bankAccountRepository;

    public DebitCardServiceImp(RepositoryFactory repositoryFactory,
                               BankAccountDebitCardRepository bankAccountDebitCardRepository,
                               MovementServiceClient movementServiceClient,
                               BankAccountRepository bankAccountRepository) {
        super(repositoryFactory);
        this.bankAccountDebitCardRepository = bankAccountDebitCardRepository;
        this.movementServiceClient = movementServiceClient;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Override
    protected Class<DebitCard> getEntityClass() {
        return DebitCard.class;
    }

    @Override
    public Mono<DebitCard> create(DebitCard document) {
        // Validaciones
        return super.create(document);
    }

    @Override
    public Mono<DebitCard> update(String idDebitCard, DebitCard debitCard) {
        return getRepository().findById(idDebitCard)
                .flatMap(debitCard1 -> {
                    debitCard1.setNumberCard(debitCard.getNumberCard());
                    debitCard1.setIdClient(debitCard.getIdClient());
                    debitCard1.setIdPrincipalAccount(debitCard.getIdPrincipalAccount());
                    return getRepository().save(debitCard1);
                });
    }

    @Override
    public Mono<List<MovementDto>> getLastTenMovementsDebitCard(String idClient) {
        return ((DebitCardRepository)getRepository()).findAllByIdClient(idClient)
                .collectList()
                .flatMap(debitCards -> {
                    List<String> idDebitCards = debitCards.stream().map(DebitCard::getId).collect(Collectors.toList());
                    return bankAccountDebitCardRepository.findAllByIdDebitCardIn(idDebitCards)
                            .collectList()
                            .flatMap(dirAccounts -> {
                                List<String> idsAccounts = dirAccounts
                                        .stream()
                                        .map(BankAccountDebitCard::getIdBankAccount)
                                        .collect(Collectors.toList());
                                return movementServiceClient.getLastTenMovements(idsAccounts);
                            });
                });

    }

    @Override
    public Mono<Map<String, Object>> getBalanceOfBankAccountInDebitCard(String idDebitCard) {
        return getRepository().findById(idDebitCard)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Debit card not found")))
                .flatMap(debitCard -> bankAccountRepository.findById(debitCard.getIdPrincipalAccount())
                        .map(bankAccount -> {
                            Map<String, Object> response = new HashMap<>();
                            response.put("balancePrincipalBankAccount", bankAccount.getBalance());
                            response.put("idBankAccount", bankAccount.getId());
                            return response;
                        }));
    }

}
