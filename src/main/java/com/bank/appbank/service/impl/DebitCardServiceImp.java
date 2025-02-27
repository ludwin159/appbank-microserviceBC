package com.bank.appbank.service.impl;

import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.DebitCard;
import com.bank.appbank.service.DebitCardService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DebitCardServiceImp extends ServiceGenImp<DebitCard, String> implements DebitCardService {

    public DebitCardServiceImp(RepositoryFactory repositoryFactory) {
        super(repositoryFactory);
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
}
