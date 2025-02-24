package com.bank.appbank.service.impl;

import com.bank.appbank.client.ConsumptionServiceClient;
import com.bank.appbank.client.PaymentServiceClient;
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.dto.ConsumptionDto;
import com.bank.appbank.model.CreditCard;
import com.bank.appbank.dto.PaymentDto;
import com.bank.appbank.repository.CreditCardRepository;
import com.bank.appbank.service.ClientService;
import com.bank.appbank.service.CreditCardService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


@Service
public class CreditCardServiceImp extends ServiceGenImp<CreditCard, String> implements CreditCardService {

    private final PaymentServiceClient paymentClientService;
    private final ConsumptionServiceClient consumptionServiceClient;
    private final ClientService clientService;

    public CreditCardServiceImp(RepositoryFactory repositoryFactory,
                                PaymentServiceClient paymentClientService,
                                ClientService clientService,
                                ConsumptionServiceClient consumptionServiceClient) {
        super(repositoryFactory);
        this.paymentClientService = paymentClientService;
        this.clientService = clientService;
        this.consumptionServiceClient = consumptionServiceClient;
    }

    @Override
    public Mono<CreditCard> findById(String id) {
        return super.findById(id)
                .flatMap(this::loadPaymentAndConsumption);
    }

    @Override
    public Mono<CreditCard> create(CreditCard creditCard) {
        return clientService.findById(creditCard.getIdClient())
                .onErrorResume(ResourceNotFoundException.class, ex ->
                        Mono.error(
                                new ResourceNotFoundException(
                                        "The client with id: "+ creditCard.getIdClient() +" does not exit.")))
                .then(super.create(creditCard));
    }

    private Mono<CreditCard> loadPaymentAndConsumption(CreditCard creditCard) {
        Mono<List<PaymentDto>> paymentsMono =
                paymentClientService.findAllPaymentByIdProductCreditAndSortByDate(creditCard.getId())
                        .collectList();
        Mono<List<ConsumptionDto>> consumptionsMono =
                consumptionServiceClient.findAllConsumptionsByIdCreditCardAndSortByDate(creditCard.getId())
                        .collectList();
        return Mono.zip(paymentsMono, consumptionsMono)
                .map(both -> {
                    List<PaymentDto> payment = both.getT1();
                    List<ConsumptionDto> consumption = both.getT2();
                    creditCard.setPayments(payment);
                    creditCard.setConsumptions(consumption);
                    return creditCard;
                });
    }

    @Override
    public Mono<CreditCard> update(String id, CreditCard creditCard) {
        return getRepository().findById(id)
                .switchIfEmpty(Mono.error(
                        new ResourceNotFoundException("The count with id: " + id + " doesn't exist!")))
                .flatMap(creditCardFound -> {
                    creditCardFound.setIdClient(creditCard.getIdClient());
                    creditCardFound.setLimitCredit(creditCard.getLimitCredit());
                    creditCardFound.setInterestRate(creditCard.getInterestRate());
                    creditCardFound.setAvailableBalance(creditCard.getAvailableBalance());
                    return getRepository().save(creditCardFound);
                });
    }

    @Override
    public Flux<CreditCard> allCreditCardsByIdClientWithPaymentAndConsumption(String idClient) {
        return ((CreditCardRepository)getRepository()).findAllByIdClient(idClient)
                .flatMap(this::loadPaymentAndConsumption);
    }

    @Override
    protected Class<CreditCard> getEntityClass() {
        return CreditCard.class;
    }
}
