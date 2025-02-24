package com.bank.appbank.service.impl;

import com.bank.appbank.client.PaymentServiceClient;
import com.bank.appbank.exceptions.CanNotDeleteEntity;
import com.bank.appbank.exceptions.CreditInvalid;
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.Client;
import com.bank.appbank.model.Credit;
import com.bank.appbank.repository.CreditRepository;
import com.bank.appbank.service.ClientService;
import com.bank.appbank.service.CreditService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CreditServiceImp extends ServiceGenImp<Credit, String> implements CreditService {

    public final ClientService clientService;
    public final PaymentServiceClient paymentServiceClient;

    public CreditServiceImp(RepositoryFactory repositoryFactory,
                            ClientService clientService,
                            PaymentServiceClient paymentServiceClient) {
        super(repositoryFactory);
        this.clientService = clientService;
        this.paymentServiceClient = paymentServiceClient;
    }

    @Override
    public Mono<Credit> findById(String id) {
        return super.findById(id)
                .flatMap(credit ->
                        paymentServiceClient.findAllPaymentByIdProductCreditAndSortByDate(id)
                                .collectList()
                                .flatMap(payments ->{
                                    credit.setPayments(payments);
                                    return Mono.just(credit);
                                })
                );
    }

    public Mono<Credit> update(String id, Credit credit) {
        return getRepository().findById(id)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("The Credit with id: " + id + " doesn't exist!")))
                .flatMap(creditFound -> {
                    creditFound.setIdClient(credit.getIdClient());
                    creditFound.setPendingBalance(credit.getPendingBalance());
                    creditFound.setInterestRate(credit.getInterestRate());
                    creditFound.setTotalAmount(credit.getTotalAmount());
                    return getRepository().save(creditFound);
                });
    }

    @Override
    public Flux<Credit> findAllCreditsByIdClient(String idClient) {
        return ((CreditRepository)getRepository()).findAllByIdClient(idClient);
    }

    @Override
    public Flux<Credit> allCreditsByIdClientWithAllPaymentsSortedByDatePayment(String idClient) {
        return findAllCreditsByIdClient(idClient)
                .flatMap(credit ->
                        paymentServiceClient.findAllPaymentByIdProductCreditAndSortByDate(credit.getId())
                                .collectList()
                                .map(payments -> {
                                    credit.setPayments(payments);
                                    return credit;
                                }));
    }

    @Override
    public Mono<Credit> create(Credit credit) {
        Mono<Client> clientFound = clientService.findById(credit.getIdClient())
                .onErrorResume((e) ->
                        Mono.error(new ResourceNotFoundException(
                                "The Client with id: " + credit.getIdClient() + " doesn't exist!")));

        Mono<Long> numberCredits = findAllCreditsByIdClient(credit.getIdClient()).count();

        return clientFound.zipWith(numberCredits).flatMap(clientWithCredits -> {
            Client client = clientWithCredits.getT1();
            Long credits = clientWithCredits.getT2();
            if (client.getTypeClient() == Client.TypeClient.PERSONAL_CLIENT && credits > 0) {
                return Mono.error(new CreditInvalid(
                        String.format("The customer %s of type: %s, already has a credit.",
                                client.getFullName(),
                                client.getTypeClient())));
            }
            return super.create(credit);
        });
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return getRepository().findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("The credit is not found")))
                .flatMap(credit -> {
                    if (credit.getPendingBalance() > 0)
                        return Mono.error(
                                new CanNotDeleteEntity("Credit can not delete while has a pending balance"));
                    return super.deleteById(credit.getId());
                })
                .then(Mono.empty());
    }

    @Override
    protected Class<Credit> getEntityClass() {
        return Credit.class;
    }
}
