package com.bank.appbank.controller;

import com.bank.appbank.model.Credit;
import com.bank.appbank.service.CreditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@WebFluxTest(CreditController.class)
class CreditControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CreditService creditService;

    @Test
    @DisplayName("Update a credit")
    void updateTest() {
        Credit credit1 = new Credit(
                "clientN001",
                500.0,
                200.0,
                0.15,
                LocalDate.now(),
                LocalDate.now(),
                12,
                0.0
        );
        credit1.setId("CREDIT001");

        when(creditService.update(eq(credit1.getId()), any())).thenReturn(Mono.just(credit1));

        webTestClient.put().uri("/credits/CREDIT001")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(credit1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalAmount").isEqualTo(500.0);

    }
}