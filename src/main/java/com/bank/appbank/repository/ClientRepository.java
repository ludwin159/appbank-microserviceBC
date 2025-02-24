package com.bank.appbank.repository;

import com.bank.appbank.model.Client;
import reactor.core.publisher.Mono;

public interface ClientRepository extends RepositoryT<Client, String> {
    public Mono<Client> findByIdentity(String identity);
    public Mono<Client> findByTaxId(String taxId);
}
