package com.bank.appbank.repository;


import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface RepositoryT <T, ID> extends ReactiveMongoRepository<T, ID> {

}
