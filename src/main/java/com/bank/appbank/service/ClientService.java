package com.bank.appbank.service;

import com.bank.appbank.model.Client;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ClientService extends ServiceT<Client, String> {
    public Mono<Client> updateClient(String id, Client client);
    public Flux<Client> findAllClientsById(List<String> ids);
}
