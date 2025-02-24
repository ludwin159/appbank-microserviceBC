package com.bank.appbank.service.impl;

import com.bank.appbank.exceptions.ClientAlreadyExist;
import com.bank.appbank.exceptions.InconsistentClientException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.BankAccount;
import com.bank.appbank.model.Client;
import com.bank.appbank.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static com.bank.appbank.model.Client.TypeClient.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @InjectMocks
    private ClientServiceImpl clientService;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private RepositoryFactory repositoryFactory;

    private Client personalClientTest, clientBusinessTest;

    @BeforeEach
    void setUp() {
        personalClientTest = new Client();
        personalClientTest.setId("clientN001");
        personalClientTest.setAddress("Jr. avenida");
        personalClientTest.setTypeClient(Client.TypeClient.PERSONAL_CLIENT);
        personalClientTest.setTaxId("");
        personalClientTest.setBusinessName("");
        personalClientTest.setEmail("ejemplo@ejemplo.com");
        personalClientTest.setFullName("Lucas Juan");
        personalClientTest.setIdentity("75690210");

        clientBusinessTest = new Client();
        clientBusinessTest.setId("clientN003");
        clientBusinessTest.setAddress("Jr. La pradera Nro 2055");
        clientBusinessTest.setTypeClient(Client.TypeClient.BUSINESS_CLIENT);
        clientBusinessTest.setTaxId("201542352");
        clientBusinessTest.setBusinessName("Doña Pepa's");
        clientBusinessTest.setPhone("+51 986532685");
        clientBusinessTest.setEmail("ejemplo@ejemplo.com");
        clientBusinessTest.setFullName("");
        clientBusinessTest.setIdentity("");
    }

    @Test
    @DisplayName("Simple update client")
    void updateSimpleClientTest() {
        when(repositoryFactory.getRepository(any())).thenReturn(clientRepository);
        Client clientUpd = new Client();
        clientUpd.setId("clientN001");
        clientUpd.setAddress("Jr. avenida");
        clientUpd.setTypeClient(Client.TypeClient.PERSONAL_CLIENT);
        clientUpd.setEmail("ejemplo@ejemplo.com");
        clientUpd.setFullName("Lucas");
        clientUpd.setIdentity("75690210");

        String idClient = "clientN001";

        // Given
        when(clientRepository.findById(idClient)).thenReturn(Mono.just(personalClientTest));
        when(clientRepository.save(personalClientTest)).thenReturn(Mono.just(personalClientTest));
        // When
        Mono<Client> clientMono = clientService.updateClient(idClient, clientUpd);
        // Then
        StepVerifier.create(clientMono)
                .expectNextMatches(client -> client.getId().equals(idClient) && client.getFullName().equals("Lucas"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Find all clients By ids")
    void findAllClientsById() {
        when(repositoryFactory.getRepository(any())).thenReturn(clientRepository);
        List<String> ids = List.of("clientN001");
        // Given
        when(clientRepository.findAllById(ids)).thenReturn(Flux.just(personalClientTest));
        // When
        Flux<Client> clientFlux = clientService.findAllClientsById(ids);
        // Then
        StepVerifier.create(clientFlux)
                .expectNextMatches(client -> client.getId().equals(ids.get(0)))
                .verifyComplete();
        verify(clientRepository).findAllById(ids);
    }

    @Test
    @DisplayName("Create a business client Correct")
    void createClientBusinessTest() {
        when(repositoryFactory.getRepository(any())).thenReturn(clientRepository);
        // Given
        when(clientRepository.findByTaxId(clientBusinessTest.getTaxId())).thenReturn(Mono.empty());
        when(clientRepository.save(any())).thenReturn(Mono.just(clientBusinessTest));
        // When
        Mono<Client> clientMono = clientService.create(clientBusinessTest);
        // Then
        StepVerifier.create(clientMono)
                .assertNext(client -> {
                    assertThat(client.getTypeClient()).isIn(BUSINESS_CLIENT, BUSINESS_PYMES_CLIENT);
                    assertThat(client.getFullName()).isEmpty();
                    assertThat(client.getIdentity()).isEmpty();
                    assertThat(client.getTaxId()).isNotEmpty();
                    assertThat(client.getBusinessName()).isNotEmpty();
                })
                .verifyComplete();
        verify(clientRepository).save(clientBusinessTest);
    }
    @Test
    @DisplayName("Create a personal client Correct")
    void createClientPersonalTest() {
        when(repositoryFactory.getRepository(any())).thenReturn(clientRepository);
        // Given
        when(clientRepository.findByIdentity(personalClientTest.getIdentity())).thenReturn(Mono.empty());
        when(clientRepository.save(any())).thenReturn(Mono.just(personalClientTest));
        // When
        Mono<Client> clientMono = clientService.create(personalClientTest);
        // Then
        StepVerifier.create(clientMono)
                .assertNext(client -> {
                    assertThat(client.getTypeClient()).isIn(PERSONAL_CLIENT, PERSONAL_VIP_CLIENT);
                    assertThat(client.getFullName()).isNotEmpty();
                    assertThat(client.getIdentity()).isNotEmpty();
                    assertThat(client.getTaxId()).isEmpty();
                    assertThat(client.getBusinessName()).isEmpty();
                })
                .verifyComplete();
        verify(clientRepository).save(personalClientTest);
    }

    @Test
    @DisplayName("Create a client business when already exist")
    void createClientWhenExistInDataBaseTest() {
        when(repositoryFactory.getRepository(any())).thenReturn(clientRepository);
        // Given
        when(clientRepository.findByTaxId(clientBusinessTest.getTaxId())).thenReturn(Mono.just(clientBusinessTest));
        // When
        Mono<Client> clientMono = clientService.create(clientBusinessTest);
        // Then
        StepVerifier.create(clientMono)
                .expectError(ClientAlreadyExist.class)
                .verify();
    }

    @Test
    @DisplayName("Create a client personal when already exist")
    void createClientPersonalWhenExistInDataBaseTest() {
        when(repositoryFactory.getRepository(any())).thenReturn(clientRepository);
        // Given
        when(clientRepository.findByIdentity(personalClientTest.getIdentity()))
                .thenReturn(Mono.just(personalClientTest));
        // When
        Mono<Client> clientMono = clientService.create(personalClientTest);
        // Then
        StepVerifier.create(clientMono)
                .expectError(ClientAlreadyExist.class)
                .verify();
    }

    @Test
    @DisplayName("Create a client personal invalid")
    void createClientPersonalInvalidTest() {
        // Given
//        personalClientTest.setTypeClient(BUSINESS_CLIENT);
        personalClientTest.setTypeClient(BUSINESS_CLIENT);
        // When
        Mono<Client> clientMono = clientService.create(personalClientTest);
        // Then
        StepVerifier.create(clientMono)
                .expectError(InconsistentClientException.class)
                .verify();
    }
    @Test
    @DisplayName("Create a client business invalid")
    void createClientBusinessInvalidTest() {
        // Given
        clientBusinessTest.setTypeClient(PERSONAL_CLIENT);
        // When
        Mono<Client> clientMono = clientService.create(clientBusinessTest);
        // Then
        StepVerifier.create(clientMono)
                .expectError(InconsistentClientException.class)
                .verify();
    }

    @Test
    @DisplayName("Create a personal vip client")
    void createVipClientTest() {
        when(repositoryFactory.getRepository(any())).thenReturn(clientRepository);
        personalClientTest.setTypeClient(PERSONAL_VIP_CLIENT);
        BankAccount newBankAccount = new BankAccount(personalClientTest.getId(),
                1500.0,
                BankAccount.TypeBankAccount.SAVING_ACCOUNT,
                2,
                0,
                0.0,
                20.0,
                0.5,
                5,
                Collections.emptyList(),
                Collections.emptyList());
        newBankAccount.setId("IDbank001");
        // Given
        when(clientRepository.findByIdentity(personalClientTest.getIdentity())).thenReturn(Mono.empty());
        when(clientRepository.save(personalClientTest)).thenReturn(Mono.just(personalClientTest));
        // When

        Mono<Client> clientMono = clientService.create(personalClientTest);
        // Then
        StepVerifier.create(clientMono)
                .assertNext(client -> {
                    assertThat("Lucas Juan").isEqualTo(client.getFullName());
                    assertThat("clientN001").isEqualTo(client.getId());
                })
                .verifyComplete();
    }
}