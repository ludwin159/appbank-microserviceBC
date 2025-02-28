package com.bank.appbank.service.impl;

import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.DebitCard;
import com.bank.appbank.repository.DebitCardRepository;
import com.bank.appbank.service.DebitCardService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Service
public class DebitCardServiceImp extends ServiceGenImp<DebitCard, String> implements DebitCardService {

    private final DebitCardRepository debitCardRepository;

    public DebitCardServiceImp(RepositoryFactory repositoryFactory, DebitCardRepository debitCardRepository) {
        super(repositoryFactory);
        this.debitCardRepository = debitCardRepository;
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
}
