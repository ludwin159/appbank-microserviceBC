package com.bank.appbank.service.impl;

import com.bank.appbank.client.MovementServiceClient;
import com.bank.appbank.dto.MovementDto;
import com.bank.appbank.dto.PaymentDto;
import com.bank.appbank.dto.ResponseAssociationWalletDto;
import com.bank.appbank.event.producer.BankAccountProducer;
import com.bank.appbank.event.producer.DebitCardProducer;
import com.bank.appbank.exceptions.*;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.BankAccount;
import com.bank.appbank.model.BankAccountDebitCard;
import com.bank.appbank.model.DebitCard;
import com.bank.appbank.model.MovementWallet;
import com.bank.appbank.repository.*;
import com.bank.appbank.service.DebitCardService;
import com.bank.appbank.utils.Numbers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DebitCardServiceImp extends ServiceGenImp<DebitCard, String> implements DebitCardService {
    private final BankAccountDebitCardRepository bankAccountDebitCardRepository;
    private final MovementServiceClient movementServiceClient;
    private final BankAccountRepository bankAccountRepository;
    private final ClientRepository clientRepository;
    private final MovementWalletRepository movementWalletRepository;
    private final DebitCardProducer debitCardProducer;

    public DebitCardServiceImp(RepositoryFactory repositoryFactory,
                               BankAccountDebitCardRepository bankAccountDebitCardRepository,
                               MovementServiceClient movementServiceClient,
                               BankAccountRepository bankAccountRepository,
                               ClientRepository clientRepository,
                               MovementWalletRepository movementWalletRepository,
                               DebitCardProducer debitCardProducer) {
        super(repositoryFactory);
        this.bankAccountDebitCardRepository = bankAccountDebitCardRepository;
        this.movementServiceClient = movementServiceClient;
        this.bankAccountRepository = bankAccountRepository;
        this.clientRepository = clientRepository;
        this.movementWalletRepository = movementWalletRepository;
        this.debitCardProducer = debitCardProducer;
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
                        .flatMap(correct -> updatePrincipalAccount(existingCard, debitCard))
                );
    }

    private Mono<String> validateClient(DebitCard existingCard, DebitCard debitCard) {
        if (!existingCard.getIdClient().equals(debitCard.getIdClient())) {
            return Mono.error(new IneligibleClientException("The change client is not available"));
        }
        return Mono.just("CORRECT");
    }

    private Mono<DebitCard> updatePrincipalAccount(DebitCard existingCard, DebitCard debitCard) {
        if (!existingCard.getIdPrincipalAccount().equals(debitCard.getIdPrincipalAccount())) {
            return findRegistersByDebitCard(existingCard.getId())
                    .flatMap(bankAccountDebitCards -> {
                        List<String> idsBanks = bankAccountDebitCards.stream()
                                .map(BankAccountDebitCard::getIdBankAccount)
                                .collect(Collectors.toList());
                        return getBankAccount(debitCard.getIdPrincipalAccount(), idsBanks);
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
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Debit card not found with ID: " + id)))
                .flatMap(existingCard -> getRepository().deleteById(existingCard.getId()));
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
    public Mono<ResponseAssociationWalletDto> validAssociationWalletToDebitCard(String idWallet, String idDebitCard) {
        return getRepository().findById(idDebitCard)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("The debit card not exist")))
                .flatMap(debitCard -> {
                    if (debitCard.getHasWallet()) {
                        return Mono.error(new ClientAlreadyExist("The debit card already has an associated wallet"));
                    }
                    return bankAccountRepository.findById(debitCard.getIdPrincipalAccount())
                            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Bank account not exist")))
                            .flatMap(bankAccount -> {
                                debitCard.setHasWallet(true);
                                ResponseAssociationWalletDto response = ResponseAssociationWalletDto.builder()
                                        .idWallet(idWallet)
                                        .idDebitCard(idDebitCard)
                                        .state("APPROVE")
                                        .balance(bankAccount.getBalance())
                                        .observation("Association Approved").build();
                                return getRepository().save(debitCard)
                                        .then(Mono.just(response));
                            });
                });
    }

    @Override
    public Mono<Void> paymentWalletWithDebitCard(List<MovementWallet> movementsWallet) {
        MovementWallet movementOrigin = movementsWallet.get(0);
        MovementWallet movementDestin = movementsWallet.get(1);
        Double amount = movementOrigin.getAmount();

        List<String> idsDebitCard = List.of(movementOrigin.getIdDebitCard(), movementDestin.getIdDebitCard());

        return ((DebitCardRepository) getRepository()).findAllByIdIn(idsDebitCard)
                .collectList()
                .flatMap(debitCards -> {
                    Optional<DebitCard> debitCardOriginOpt = debitCards.stream()
                            .filter(d -> d.getId().equals(movementOrigin.getIdDebitCard()))
                            .findFirst();
                    Optional<DebitCard> debitCardDestinOpt = debitCards.stream()
                            .filter(d -> d.getId().equals(movementDestin.getIdDebitCard()))
                            .findFirst();

                    List<String> idsPrincipalBankAccount = new ArrayList<>();
                    if (debitCardOriginOpt.isPresent()) {
                        movementOrigin.setStateMovement(MovementWallet.StateMovement.APPROVE);
                        idsPrincipalBankAccount.add(debitCardOriginOpt.get().getIdPrincipalAccount());
                    }

                    if (debitCardDestinOpt.isPresent()) {
                        movementDestin.setStateMovement(MovementWallet.StateMovement.APPROVE);
                        idsPrincipalBankAccount.add(debitCardDestinOpt.get().getIdPrincipalAccount());
                    }
                    return bankAccountRepository.findByIdIn(idsPrincipalBankAccount)
                            .collectList()
                            .flatMap(bankAccounts -> {
                                Optional<BankAccount> bankAccountOriginOpt = debitCardOriginOpt.flatMap(dc ->
                                        bankAccounts.stream()
                                                .filter(ba -> ba.getId().equals(dc.getIdPrincipalAccount()))
                                                .findFirst()
                                );

                                Optional<BankAccount> bankAccountDestinOpt = debitCardDestinOpt.flatMap(dc ->
                                        bankAccounts.stream()
                                                .filter(ba -> ba.getId().equals(dc.getIdPrincipalAccount()))
                                                .findFirst()
                                );

                                Mono<BankAccount> bankAccountOriginMono = Mono.justOrEmpty(bankAccountOriginOpt)
                                        .flatMap(bankAccount -> {
                                            double newBalance = Numbers.round(bankAccount.getBalance() - amount);
                                            if (newBalance < 0)
                                                return Mono.error(
                                                    new UnsupportedMovementException("Balance is not supported"));
                                            bankAccount.setBalance(newBalance);
                                            return bankAccountRepository.save(bankAccount);
                                        });

                                Mono<BankAccount> bankAccountDestinMono = bankAccountDestinOpt.map(bankAccount -> {
                                    bankAccount.setBalance(Numbers.round(bankAccount.getBalance() + amount));
                                    return bankAccountRepository.save(bankAccount);
                                }).orElse(Mono.empty());

                                return Mono.when(
                                        bankAccountOriginMono,bankAccountDestinMono)
                                        .doOnSuccess(ignored -> log.info("Bank accounts processed"))
                                        .doOnError(error -> log.error("Error in Mono.when(): ", error))
                                        .then(Mono.zip(
                                                    movementWalletRepository.save(movementOrigin),
                                                    movementWalletRepository.save(movementDestin)
                                            ).flatMap(tuple -> {
                                                MovementWallet movementOri = tuple.getT1();
                                                MovementWallet movementDes = tuple.getT2();
                                                movementOri.setStateMovement(MovementWallet.StateMovement.APPROVE);
                                                movementDes.setStateMovement(MovementWallet.StateMovement.APPROVE);
                                                List<MovementWallet> movements = List.of(movementOri, movementDes);
                                                debitCardProducer.publishConfirmationPaymentDebit(movements);
                                                return Mono.just(movements);
                                            }).then()
                                        );
                            });
                })
                .onErrorResume(error -> {
                    movementOrigin.setStateMovement(MovementWallet.StateMovement.REJECTED);
                    movementOrigin.setDescription(error.getMessage());
                    movementDestin.setStateMovement(MovementWallet.StateMovement.REJECTED);
                    movementDestin.setDescription(error.getMessage());
                    List<MovementWallet> errorMovements = List.of(movementOrigin, movementDestin);

                    return Mono.when(
                            movementWalletRepository.save(movementOrigin),
                            movementWalletRepository.save(movementDestin)
                    ).then(Mono.fromRunnable(() ->
                                    debitCardProducer.publishConfirmationPaymentDebit(errorMovements)))
                            .then();
                });
    }


    @Override
    protected Class<DebitCard> getEntityClass() {
        return DebitCard.class;
    }
}
