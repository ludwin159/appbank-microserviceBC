package com.bank.appbank.service.impl;

import com.bank.appbank.client.MovementServiceClient;
import com.bank.appbank.exceptions.*;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.BankAccount;
import com.bank.appbank.model.Client;
import com.bank.appbank.dto.MovementDto;
import com.bank.appbank.repository.BankAccountRepository;
import com.bank.appbank.repository.CreditCardRepository;
import com.bank.appbank.service.BankAccountService;
import com.bank.appbank.service.ClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bank.appbank.model.BankAccount.TypeBankAccount.*;
import static com.bank.appbank.model.Client.TypeClient.*;

@Service
public class BankAccountServiceImp extends ServiceGenImp<BankAccount, String> implements BankAccountService {

    private static final Logger log = LoggerFactory.getLogger(BankAccountServiceImp.class);
    private final ClientService clientService;
    private final MovementServiceClient movementServiceClient;
    private final CreditCardRepository creditCardRepository;

    public BankAccountServiceImp(RepositoryFactory repositoryFactory,
                                 ClientService clientService,
                                 MovementServiceClient movementServiceClient,
                                 CreditCardRepository creditCardRepository) {
        super(repositoryFactory);
        this.clientService = clientService;
        this.movementServiceClient = movementServiceClient;
        this.creditCardRepository = creditCardRepository;
    }


    @Override
    public Mono<BankAccount> findById(String id) {
        return super.findById(id)
                .flatMap(this::uploadMovementsAndTransfers)
                .onErrorResume(error -> {
                    log.error(error.getMessage());
                    return Mono.error(error);
                });
    }

    private Mono<BankAccount> uploadMovementsAndTransfers(BankAccount bankAccount) {
        Flux<MovementDto> movementsFlux =
                movementServiceClient.getMovementsByBankAccountIdInPresentMonth(bankAccount.getId());
        return movementsFlux.collectList()
                .map(movements -> {
                    bankAccount.setMovements(movements);
                    return bankAccount;
                });
    }

    @Override
    public Mono<BankAccount> create(BankAccount bankAccount) {

        Mono<Client> clientFound = clientService.findById(bankAccount.getIdClient())
                .flatMap(client -> validateClientAndBankAccount(bankAccount, client))
                .onErrorResume(ResourceNotFoundException.class, e ->
                        Mono.error(new ResourceNotFoundException(
                                "The client with id " + bankAccount.getIdClient() + " doesn't exist")));

        Mono<List<BankAccount>> currentAccounts = findBankAccountsByIdClient(bankAccount.getIdClient())
                .filter(bankAccountFound ->
                        bankAccountFound.getTypeBankAccount().equals(bankAccount.getTypeBankAccount()))
                .collectList();

        return clientFound.zipWith(currentAccounts)
                .flatMap(clientWithAccounts -> {
                    Client client = clientWithAccounts.getT1();
                    List<BankAccount> accountsClient = clientWithAccounts.getT2();

                    if (isPersonalClientWithMultipleAccounts(client, accountsClient)) {
                        String messageError = String.format(
                                "The client: %s of type %s, has reached the limit number of allowed accounts.",
                                client.getFullName(), client.getTypeClient());
                        log.error(messageError);
                        return Mono.error(new LimitAccountsException(messageError));
                    }
                    log.info("Bank account created! " + bankAccount);
                    return super.create(bankAccount);
                });
}


    private Mono<Client> validateClientAndBankAccount(BankAccount bankAccount, Client client) {
        if (isPersonalVipClientWithSavingAccount(bankAccount, client) ||
                isBusinessClientPymeWithCurrentAccount(bankAccount, client))
            return validateClientWithCreditCard(client);

        if (isBusinessClientWithInvalidConditions(bankAccount, client)) {
            return Mono.error(new IneligibleClientException(
                    String.format("The client: %s of type: %s only can have current accounts.",
                            client.getBusinessName(), client.getTypeClient())));
        }
        if (isBusinessClientWithoutHolders(bankAccount, client)) {
            return Mono.error(new IneligibleClientException(
                    String.format("The client: %s of type: %s must have at least a holder.",
                            client.getBusinessName(), client.getTypeClient())));
        }

        if (hasValidAccountHoldersOrSignatories(bankAccount)) {
            return validateAccountHoldersAndSignatories(bankAccount, client);
        }

        return Mono.just(client);
    }

    private boolean isPersonalVipClientWithSavingAccount(BankAccount bankAccount, Client client) {
        Client.TypeClient typeClient = client.getTypeClient();
        BankAccount.TypeBankAccount typeBankAccount = bankAccount.getTypeBankAccount();

        return (typeClient == PERSONAL_VIP_CLIENT) && (typeBankAccount == SAVING_ACCOUNT);
    }

    private boolean isBusinessClientPymeWithCurrentAccount(BankAccount bankAccount, Client client) {
        Client.TypeClient typeClient = client.getTypeClient();
        BankAccount.TypeBankAccount typeBankAccount = bankAccount.getTypeBankAccount();

        return (typeClient == BUSINESS_PYMES_CLIENT) && (typeBankAccount == CURRENT_ACCOUNT);
    }

    private Mono<Client> validateClientWithCreditCard(Client client) {
        return creditCardRepository.findAllByIdClient(client.getId())
                .count()
                .flatMap(numberCreditCards -> {
                    if (numberCreditCards > 0) {
                        return Mono.just(client);
                    }
                    String messageError = String.format(
                            "The customer %s of type %s must have at least one credit card.",
                            client.getFullName(), client.getTypeClient());
                    log.error(messageError);
                    return Mono.error(
                            new IneligibleClientException(messageError));
                });
    }

    private boolean isBusinessClientWithInvalidConditions(BankAccount bankAccount, Client client) {
        Client.TypeClient typeClient = client.getTypeClient();
        BankAccount.TypeBankAccount typeBankAccount = bankAccount.getTypeBankAccount();

        return (typeClient == BUSINESS_CLIENT) && (typeBankAccount != CURRENT_ACCOUNT);
    }

    private boolean isBusinessClientWithoutHolders(BankAccount bankAccount, Client client) {
        Client.TypeClient typeClient = client.getTypeClient();
        List<String> holdersAccount = bankAccount.getAccountHolders();
        return (typeClient == BUSINESS_CLIENT) && holdersAccount.isEmpty();
    }

    private boolean hasValidAccountHoldersOrSignatories(BankAccount bankAccount) {
        List<String> holdersAccount = bankAccount.getAccountHolders();
        List<String> authorizedSignatorits = bankAccount.getAuthorizedSignatorits();
        return !holdersAccount.isEmpty() || !authorizedSignatorits.isEmpty();
    }

    private Mono<Client> validateAccountHoldersAndSignatories(BankAccount bankAccount, Client client) {
        List<String> uniqueIds = Stream.concat(bankAccount.getAccountHolders().stream(),
                        bankAccount.getAuthorizedSignatorits().stream())
                .distinct()
                .collect(Collectors.toList());

        return clientService.findAllClientsById(uniqueIds)
                .collectList()
                .flatMap(clientsFound -> {
                    Set<String> idsFound = clientsFound.stream()
                            .map(Client::getId)
                            .collect(Collectors.toSet());
                    Set<String> idsNotFound = new HashSet<>(uniqueIds);
                    idsNotFound.removeAll(idsFound);

                    if (!idsNotFound.isEmpty()) {
                        return Mono.error(new ClientNotFoundException(
                                "The following clients with ids: " + idsNotFound + " were not found."));
                    }

                    return Mono.just(client);
                });
    }

    private boolean isPersonalClientWithMultipleAccounts(Client client, List<BankAccount> accountsClient) {
        List<BankAccount> savingAccounts = accountsClient.stream()
                .filter(account -> account.getTypeBankAccount() == SAVING_ACCOUNT)
                .collect(Collectors.toList());
        List<BankAccount> accountsCurrent = accountsClient.stream()
                .filter(account -> account.getTypeBankAccount() == CURRENT_ACCOUNT)
                .collect(Collectors.toList());

        return client.getTypeClient() == PERSONAL_CLIENT &&
                (!accountsCurrent.isEmpty() || !savingAccounts.isEmpty());
    }

    @Override
    public Mono<BankAccount> update(String id, BankAccount bankAccount) {
        return getRepository().findById(id)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("The count with id: " + id + " doesn't exist!")))
                .flatMap(accountFound -> {
                    if (isChangeClient(bankAccount, accountFound)) {
                        return validateClientInBankAccount(accountFound, bankAccount)
                                .flatMap(bankAccountCorrect -> {
                                    bankAccountCorrect.setIdClient(bankAccount.getIdClient());
                                    return getRepository().save(bankAccountCorrect);
                                });
                    }
                    BankAccount updatedBankAccount = updateFilesBankAccount(accountFound, bankAccount);
                    return getRepository().save(updatedBankAccount);
                });
    }

    private boolean isChangeClient(BankAccount bankAccount1, BankAccount bankAccount2) {
        return !bankAccount1.getIdClient().equals(bankAccount2.getIdClient());
    }

    private Mono<BankAccount> validateClientInBankAccount(BankAccount oldBankAccount, BankAccount newBankAccount) {
        Mono<Client> newClientMono = clientService.findById(newBankAccount.getIdClient());
        Mono<Client> oldClientMono = clientService.findById(oldBankAccount.getIdClient());
        return Mono.zip(newClientMono, oldClientMono)
                .flatMap(both -> {
                   Client client = both.getT1();
                   Client newClient = both.getT2();
                   if (!client.getTypeClient().equals(newClient.getTypeClient()))
                       return Mono.error(new IneligibleClientException("Both clients are not the same type."));
                    return Mono.just(updateFilesBankAccount(oldBankAccount, newBankAccount));
                })
                .onErrorResume(ResourceNotFoundException.class,
                        ex -> Mono.error(
                                new ClientNotFoundException("The new client was not found.")));
    }

    public BankAccount updateFilesBankAccount(BankAccount oldBankAccount, BankAccount newBankAccount) {
        oldBankAccount.setBalance(newBankAccount.getBalance());
        oldBankAccount.setMovements(newBankAccount.getMovements());
        oldBankAccount.setMaintenanceCost(newBankAccount.getMaintenanceCost());
        oldBankAccount.setAccountHolders(newBankAccount.getAccountHolders());
        oldBankAccount.setAuthorizedSignatorits(newBankAccount.getAuthorizedSignatorits());
        oldBankAccount.setExpirationDate(newBankAccount.getExpirationDate());
        oldBankAccount.setLimitMovements(newBankAccount.getLimitMovements());
        return oldBankAccount;
    }

    @Override
    public Flux<BankAccount> findBankAccountsByIdClientWithAllMovementsSortedByDate(String idClient) {
        return findBankAccountsByIdClient(idClient)
                .flatMap(bankAccount ->
                        movementServiceClient.getAllMovementsByIdBankAccountAndSortByDate(bankAccount.getId())
                                    .collectList()
                                    .map(accountMovementos -> {
                                        bankAccount.setMovements(accountMovementos);
                                        return bankAccount;
                                    }));
    }


    @Override
    public Flux<BankAccount> findBankAccountsByIdClient(String idClient) {
        return ((BankAccountRepository) getRepository()).findAllByIdClient(idClient);
    }

    @Override
    protected Class<BankAccount> getEntityClass() {
        return BankAccount.class;
    }

}
