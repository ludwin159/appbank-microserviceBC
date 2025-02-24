package com.bank.appbank.service.impl;

import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.dto.ConsumptionDto;
import com.bank.appbank.repository.RepositoryT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceGenImpTest {

    @Mock
    private RepositoryFactory repositoryFactory;

    @Mock
    private RepositoryT<ConsumptionDto, String> repository;

    private ServiceGenImp<ConsumptionDto, String> service;
    private ConsumptionDto consumption1;
    private ConsumptionDto consumption2;

    @BeforeEach
    void setUp() {
        when(repositoryFactory.getRepository(ConsumptionDto.class)).thenReturn(repository);
        service = new ServiceGenImp<>(repositoryFactory) {
            @Override
            protected Class<ConsumptionDto> getEntityClass() {
                return ConsumptionDto.class;
            }
        };

        consumption1 = ConsumptionDto.builder()
                .id("asdfasd3fas6df5a6sd5")
                .amount(300.0)
                .dateConsumption(LocalDateTime.now())
                .description("The first consumption")
                .idCreditCard("asdf3as5df6a5sd6f5aa").build();

        consumption2 = ConsumptionDto.builder()
                .id("asdfasd3fas6df5a6sd5")
                .amount(100.0)
                .dateConsumption(LocalDateTime.now())
                .description("The first consumption")
                .idCreditCard("asdf3as5df6a5sd6f5aa").build();
    }

    @Test
    @DisplayName("Find all documents in a collection")
    void getAllTest() {
        when(repository.findAll()).thenReturn(Flux.just(consumption1, consumption2));

        StepVerifier.create(service.getAll())
                .expectNextMatches(element1 -> element1.getId().equals("asdfasd3fas6df5a6sd5"))
                .expectNextMatches(element2 -> element2.getAmount() == 100.0)
                .expectComplete();
        verify(repository).findAll();
    }

    @Test
    @DisplayName("Find a document by id")
    void findByIdTest() {
        // Given
        String idFound = "asdfasd3fas6df5a6sd5";
        when(repository.findById(idFound)).thenReturn(Mono.just(consumption1));
        // When
        Mono<ConsumptionDto> consumptionMono = service.findById(idFound);
        // Then
        StepVerifier.create(consumptionMono)
                .expectNextMatches(element -> element.getId().equals("asdfasd3fas6df5a6sd5"))
                .expectComplete();
        verify(repository).findById(idFound);
    }

    @Test
    @DisplayName("Not exists a resource")
    void findByIdAndNotExists() {
        // Given
        String idNotExist = "abc";
        when(repository.findById(idNotExist)).thenReturn(Mono.empty());
        // WHen
        StepVerifier.create(service.findById(idNotExist))
                .expectError(ResourceNotFoundException.class)
                .verify();

        verify(repository).findById(idNotExist);
    }

    @Test
    @DisplayName("Create a document")
    void createTest() {
        // Given
        when(repository.save(consumption2)).thenReturn(Mono.just(consumption2));
        // When
        Mono<ConsumptionDto> consumptionSaved = service.create(consumption2);
        // Then
        StepVerifier.create(consumptionSaved)
                .expectNextMatches(element1 -> element1.getAmount() == 100.0)
                .expectComplete();
        verify(repository, times(1)).save(consumption2);
    }

    @Test
    @DisplayName("Delete a document")
    void deleteNotExistById() {
        // Given
        String idDelete = consumption2.getId();
        when(repository.deleteById(idDelete)).thenReturn(Mono.empty());
        // When
        StepVerifier.create(service.deleteById(idDelete))
                .verifyComplete();
    }

}