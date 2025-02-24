package com.bank.appbank.controller;

import com.bank.appbank.model.BankAccount;
import com.bank.appbank.service.BankAccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@WebFluxTest(BankAccountController.class)
class BankAccountControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private BankAccountService bankAccountService;

    @Test
    @DisplayName("Update a bank account")
    void update() {
        BankAccount bankAccount1 = new BankAccount("clientN001",
                1500.0,
                BankAccount.TypeBankAccount.SAVING_ACCOUNT,
                2,
                0,
                0.0,
                0.2,
                0.5,
                5,
                Collections.emptyList(),
                Collections.emptyList());
        bankAccount1.setId("IDbank001");
        when(bankAccountService.update(eq("IDbank001"), any())).thenReturn(Mono.just(bankAccount1));

        webTestClient.put().uri("/bank-accounts/{id}", bankAccount1.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bankAccount1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("IDbank001");

    }
}