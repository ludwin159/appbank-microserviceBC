package com.bank.appbank.exceptions.handler;

import com.bank.appbank.controller.ClientController;
import com.bank.appbank.exceptions.IneligibleClientException;
import com.bank.appbank.exceptions.InsufficientBalance;
import com.bank.appbank.exceptions.LimitAccountsException;
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.model.Client;
import com.bank.appbank.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@WebFluxTest(ClientController.class)
@Import(ExceptionHandlers.class)
class ExceptionHandlersTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ClientService clientService;

    private ExceptionHandlers exceptionHandlers;
    private Client personalClient;

    @BeforeEach
    void setUp() {
        exceptionHandlers = new ExceptionHandlers();
        personalClient = new Client();
        personalClient.setId("clientN001");
        personalClient.setAddress("Jr. avenida");
        personalClient.setTypeClient(Client.TypeClient.PERSONAL_CLIENT);
        personalClient.setEmail("ejemplo@ejemplo.com");
        personalClient.setFullName("Lucas Juan");
        personalClient.setBusinessName("");
        personalClient.setPhone("");
        personalClient.setTaxId("");
        personalClient.setIdentity("75690210");
    }
    @Test
    void testHandleResourceNotFoundException() {
        String idNotFound = "clientN001";
        when(clientService.updateClient(idNotFound, personalClient))
                .thenReturn(Mono.error(new ResourceNotFoundException("Resource not found")));

        webTestClient.put().uri("/client/clientN001")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(personalClient)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Resource not found");
    }

    @Test
    @DisplayName("When a client is not ok")
    void ineligibleClientExceptionTest() {
        when(clientService.updateClient(personalClient.getId(), personalClient))
                .thenReturn(Mono
                    .error(new IneligibleClientException("The client business only can have current accounts")));
        webTestClient.put().uri("/client/{id}", personalClient.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(personalClient)
                .exchange()
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo("The client business only can have current accounts");
    }

}