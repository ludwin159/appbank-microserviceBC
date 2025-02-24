package com.bank.appbank.controller;

import com.bank.appbank.model.Client;
import com.bank.appbank.service.ClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@WebFluxTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ClientService clientService;

    @Test
    @DisplayName("Update client controller")
    void updateClient() {
        Client personalClient = new Client();
        personalClient.setId("clientN001");
        personalClient.setAddress("Jr. avenida");
        personalClient.setTypeClient(Client.TypeClient.PERSONAL_CLIENT);
        personalClient.setEmail("ejemplo@ejemplo.com");
        personalClient.setFullName("Lucas Juan");
        personalClient.setBusinessName("");
        personalClient.setPhone("");
        personalClient.setTaxId("");
        personalClient.setIdentity("75690210");

        when(clientService.updateClient("clientN001", personalClient)).thenReturn(Mono.just(personalClient));

        webTestClient.put().uri("/client/clientN001")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(personalClient)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("clientN001")
                .jsonPath("$.fullName").isEqualTo("Lucas Juan");
    }
}