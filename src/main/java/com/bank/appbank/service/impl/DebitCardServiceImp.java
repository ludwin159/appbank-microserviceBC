package com.bank.appbank.service.impl;

<<<<<<< HEAD
import com.bank.appbank.client.MovementServiceClient;
import com.bank.appbank.dto.MovementDto;
import com.bank.appbank.dto.PaymentDto;
=======
>>>>>>> d08731159d5f7028eaa5e24176376745c88efd75
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.BankAccountDebitCard;
import com.bank.appbank.model.DebitCard;
<<<<<<< HEAD
import com.bank.appbank.repository.BankAccountDebitCardRepository;
import com.bank.appbank.repository.BankAccountRepository;
=======
>>>>>>> d08731159d5f7028eaa5e24176376745c88efd75
import com.bank.appbank.repository.DebitCardRepository;
import com.bank.appbank.service.DebitCardService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

<<<<<<< HEAD
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
=======
import java.util.Collections;
>>>>>>> d08731159d5f7028eaa5e24176376745c88efd75

@Service
public class DebitCardServiceImp extends ServiceGenImp<DebitCard, String> implements DebitCardService {
    private final BankAccountDebitCardRepository bankAccountDebitCardRepository;
    private final MovementServiceClient movementServiceClient;
    private final BankAccountRepository bankAccountRepository;

<<<<<<< HEAD
    public DebitCardServiceImp(RepositoryFactory repositoryFactory,
                               BankAccountDebitCardRepository bankAccountDebitCardRepository,
                               MovementServiceClient movementServiceClient,
                               BankAccountRepository bankAccountRepository) {
        super(repositoryFactory);
        this.bankAccountDebitCardRepository = bankAccountDebitCardRepository;
        this.movementServiceClient = movementServiceClient;
        this.bankAccountRepository = bankAccountRepository;
=======
    private final DebitCardRepository debitCardRepository;

    public DebitCardServiceImp(RepositoryFactory repositoryFactory, DebitCardRepository debitCardRepository) {
        super(repositoryFactory);
        this.debitCardRepository = debitCardRepository;
>>>>>>> d08731159d5f7028eaa5e24176376745c88efd75
    }

    @Override
    protected Class<DebitCard> getEntityClass() {
        return DebitCard.class;
    }

    @Override
    public Mono<DebitCard> updateDebitCard(String id, DebitCard updatedCard) {
        return debitCardRepository.findById(id)
                .flatMap(existingCard -> {
                    existingCard.setNumberCard(updatedCard.getNumberCard());
                    existingCard.setIdClient(updatedCard.getIdClient());
                    existingCard.setIdPrincipalAccount(updatedCard.getIdPrincipalAccount());
                    existingCard.setBankAccounts(updatedCard.getBankAccounts());
                    return debitCardRepository.save(existingCard);
                })
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Debit card not found with ID: " + id)));
    }

    @Override
    public Mono<DebitCard> create(DebitCard newCard) {
        return checkIfDebitCardExists(newCard.getNumberCard())
                .flatMap(exists -> {
                    if ("NOT_EXISTS".equals(exists)) {
                        return debitCardRepository.save(newCard);
                    }
                });
    }

    private Mono<String> checkIfDebitCardExists(String cardNumber) {
        return debitCardRepository.findById(cardNumber)
                .map(existing -> "EXIST")
                .defaultIfEmpty("NOT_EXISTS");
    }

    @Override
    public Mono<DebitCard> update(String idDebitCard, DebitCard debitCard) {
        return debitCardRepository.findById(idDebitCard)
                .flatMap(existingCard -> {
                    existingCard.setNumberCard(debitCard.getNumberCard());
                    existingCard.setIdClient(debitCard.getIdClient());
                    existingCard.setIdPrincipalAccount(debitCard.getIdPrincipalAccount());
                    existingCard.setBankAccounts(debitCard.getBankAccounts());
                    return debitCardRepository.save(existingCard);
                })
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Debit card not found with ID: " + id)));
    }

    @Override
    public Flux<DebitCard> findAllByClientId(String clientId) {
        return debitCardRepository.findAllById(Collections.singleton(clientId));
    }

    @Override
    public Mono<Void> deleteDebitCard(String id) {
        return debitCardRepository.findById(id)
                .flatMap(existingCard -> debitCardRepository.delete(existingCard))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Debit card not found with ID: " + id)));
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
