package com.bank.appbank.service.impl;

import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.DebitCard;
import com.bank.appbank.service.DebitCardService;
import org.springframework.stereotype.Service;

@Service
public class DebitCardServiceImp extends ServiceGenImp<DebitCard, String> implements DebitCardService {

    public DebitCardServiceImp(RepositoryFactory repositoryFactory) {
        super(repositoryFactory);
    }

    @Override
    protected Class<DebitCard> getEntityClass() {
        return DebitCard.class;
    }
}
