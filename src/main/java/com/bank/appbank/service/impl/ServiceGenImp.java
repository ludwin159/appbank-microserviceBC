package com.bank.appbank.service.impl;


import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.repository.RepositoryT;
import com.bank.appbank.service.ServiceT;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class ServiceGenImp<T, ID> implements ServiceT<T, ID> {

    private final RepositoryFactory repositoryFactory;
    protected abstract Class<T> getEntityClass();

    public ServiceGenImp(RepositoryFactory repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }

    protected RepositoryT<T, ID> getRepository() {
        return repositoryFactory.getRepository(getEntityClass());
    }

    @Override
    public Flux<T> getAll() {
        return getRepository().findAll();
    }

    @Override
    public Mono<T> findById(ID id) {
        return getRepository().findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("the resource doesn't exists")));
    }

    @Override
    public Mono<T> create(T document) {
        return getRepository().save(document);
    }

    @Override
    public Mono<Void> deleteById(ID id) {
        return getRepository().deleteById(id);
    }
}
