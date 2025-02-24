package com.bank.appbank.factory;

import com.bank.appbank.model.*;
import com.bank.appbank.repository.*;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RepositoryFactory {

    private final Map<Class<?>, ReactiveMongoRepository<?, String>> repositoryMap;

    public RepositoryFactory(
            ClientRepository clientRepository,
            CreditCardRepository creditCardRepository,
            CreditRepository creditRepository,
            BankAccountRepository bankAccountRepository
    ) {
        this.repositoryMap = Map.of(
                Client.class, clientRepository,
                CreditCard.class, creditCardRepository,
                Credit.class, creditRepository,
                BankAccount.class, bankAccountRepository
        );
    }

    @SuppressWarnings("unchecked")
    public <T, ID, R extends ReactiveMongoRepository<T, ID>> R getRepository(Class<T> tClass) {
        return (R)repositoryMap.get(tClass);
    }
}
