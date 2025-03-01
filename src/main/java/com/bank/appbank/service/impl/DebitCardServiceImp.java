package com.bank.appbank.service.impl;

import com.bank.appbank.client.MovementServiceClient;
import com.bank.appbank.dto.MovementDto;
import com.bank.appbank.exceptions.BadInformationException;
import com.bank.appbank.exceptions.IneligibleClientException;
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.BankAccount;
import com.bank.appbank.model.BankAccountDebitCard;
import com.bank.appbank.model.DebitCard;
import com.bank.appbank.repository.BankAccountDebitCardRepository;
import com.bank.appbank.repository.BankAccountRepository;
import com.bank.appbank.repository.ClientRepository;
import com.bank.appbank.repository.DebitCardRepository;
import com.bank.appbank.service.DebitCardService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DebitCardServiceImp extends ServiceGenImp<DebitCard, String> implements DebitCardService {
    private final BankAccountDebitCardRepository bankAccountDebitCardRepository;
    private final MovementServiceClient movementServiceClient;
    private final BankAccountRepository bankAccountRepository;
    private final ClientRepository clientRepository;

    public DebitCardServiceImp(RepositoryFactory repositoryFactory,
                               BankAccountDebitCardRepository bankAccountDebitCardRepository,
                               MovementServiceClient movementServiceClient,
                               BankAccountRepository bankAccountRepository,
                               ClientRepository clientRepository) {
        super(repositoryFactory);
        this.bankAccountDebitCardRepository = bankAccountDebitCardRepository;
        this.movementServiceClient = movementServiceClient;
        this.bankAccountRepository = bankAccountRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public Mono<DebitCard> create(DebitCard newDebitCard) {
        return clientRepository.findById(newDebitCard.getIdClient())
                .flatMap(client -> bankAccountRepository.findById(newDebitCard.getIdPrincipalAccount())
                        .flatMap(bankAccount -> getRepository().save(newDebitCard)
                                .flatMap(debitCard -> {
                                    BankAccountDebitCard register = new BankAccountDebitCard();
                                    register.setIdBankAccount(bankAccount.getId());
                                    register.setIdDebitCard(debitCard.getId());
                                    return bankAccountDebitCardRepository.save(register)
                                            .then(Mono.just(debitCard));
                                })));
    }


    @Override
    public Mono<DebitCard> update(String idDebitCard, DebitCard debitCard) {
        return getRepository().findById(idDebitCard)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Debit card not found with ID: " + idDebitCard)))
                .flatMap(existingCard -> validateClient(existingCard, debitCard)
                        .then(updatePrincipalAccount(existingCard, debitCard))
                );
    }

    private Mono<Void> validateClient(DebitCard existingCard, DebitCard debitCard) {
        if (!existingCard.getIdClient().equals(debitCard.getIdClient())) {
            return Mono.error(new IneligibleClientException("The change client is not available"));
        }
        return Mono.empty();
    }

    private Mono<DebitCard> updatePrincipalAccount(DebitCard existingCard, DebitCard debitCard) {
        if (!existingCard.getIdPrincipalAccount().equals(debitCard.getIdPrincipalAccount())) {
            return findRegistersByDebitCard(existingCard.getId())
                    .flatMap(bankAccountDebitCards -> {
                        List<String> idsBanks = bankAccountDebitCards.stream()
                                .map(BankAccountDebitCard::getIdBankAccount)
                                .collect(Collectors.toList());
                        return getBankAccount(existingCard.getId(), idsBanks);
                    })
                    .flatMap(existId -> saveUpdatedCard(existingCard, debitCard));
        }
        return saveUpdatedCard(existingCard, debitCard);
    }

    private Mono<DebitCard> saveUpdatedCard(DebitCard existingCard, DebitCard debitCard) {
        existingCard.setNumberCard(debitCard.getNumberCard());
        existingCard.setIdPrincipalAccount(debitCard.getIdPrincipalAccount());
        return getRepository().save(existingCard);
    }

    private Mono<List<BankAccountDebitCard>> findRegistersByDebitCard(String idDebitCard) {
        return bankAccountDebitCardRepository.findAllByIdDebitCard(idDebitCard).collectList();
    }

    private Mono<String> getBankAccount(String idAccountToFind, List<String> idBankAccounts) {
        return Mono.justOrEmpty(findStringInStrings(idAccountToFind, idBankAccounts))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("The new main account is not related")));
    }

    private Optional<String> findStringInStrings(String idAccountToFind, List<String> idBankAccounts) {
        return idBankAccounts.stream()
                .filter(idBankAccount -> idBankAccount.equals(idAccountToFind))
                .findFirst();
    }

    @Override
    public Mono<Void> deleteDebitCard(String id) {
        return getRepository().findById(id)
                .flatMap(existingCard -> getRepository().delete(existingCard))
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

    @Override
    public Mono<BankAccountDebitCard> addBankAccountToDebitCard(BankAccountDebitCard bankAccountDebitCard) {
        String idBankAccount = bankAccountDebitCard.getIdBankAccount();
        String idDebitCard = bankAccountDebitCard.getIdDebitCard();
        String messageError = "The bank account is already related to the debit card";

        Mono<BankAccount> bankAccountMono = bankAccountRepository.findById(idBankAccount)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Bank account not exist with id: " + idBankAccount)));

        Mono<DebitCard> debitCardMono = getRepository().findById(idDebitCard)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("Debit card not exist with id: " + idDebitCard)));

        return Mono.zip(bankAccountMono, debitCardMono)
                .flatMap(tuple -> {
                    BankAccount bankAccount = tuple.getT1();
                    DebitCard debitCard = tuple.getT2();
                    if (!Objects.equals(bankAccount.getIdClient(), debitCard.getIdClient())){
                        String message = "The bank account client does not match the debit account: (" +
                        bankAccount.getIdClient() + " : " + debitCard.getIdClient() + ")";
                        return Mono.error(new IneligibleClientException(message));
                    }
                    return bankAccountDebitCardRepository.findAllByIdDebitCard(debitCard.getId())
                            .map(BankAccountDebitCard::getIdBankAccount)
                            .collectList()
                            .flatMap(idsBankAccount -> {
                                if (findStringInStrings(idBankAccount, idsBankAccount).isPresent()) {
                                    return Mono.error(new BadInformationException(messageError));
                                }
                                return bankAccountDebitCardRepository.save(bankAccountDebitCard);
                            });
                });
    }

    @Override
    public Mono<DebitCard> findByIdWithBankAccountsOrderByCreatedAt(String idDebitCard) {
        return getRepository().findById(idDebitCard)
                .switchIfEmpty(
                        Mono.error(new ResourceNotFoundException("Debit card not exist with id: " + idDebitCard)))
                .flatMap(debitCard -> bankAccountDebitCardRepository
                        .findAllByIdDebitCardOrderByCreatedAtAsc(idDebitCard)
                        .collectList()
                        .flatMap(registers -> {
                            List<String> bankAccountsIdsInOrder = registers.stream()
                                    .map(BankAccountDebitCard::getIdBankAccount)
                                    .collect(Collectors.toList());
                            return bankAccountRepository.findByIdIn(bankAccountsIdsInOrder)
                                    .collectList()
                                    .map(bankAccounts -> {
                                        Map<String, BankAccount> mapBankAccounts = bankAccounts.stream()
                                                .collect(Collectors.toMap(BankAccount::getId, account -> account));

                                        List<BankAccount> listBankAccounts = bankAccountsIdsInOrder
                                                .stream()
                                                .map(mapBankAccounts::get)
                                                .filter(Objects::nonNull)
                                                .collect(Collectors.toList());

                                       debitCard.setBankAccounts(listBankAccounts);
                                       return debitCard;
                                    });
                        })
                );
    }

    @Override
    protected Class<DebitCard> getEntityClass() {
        return DebitCard.class;
    }
}
