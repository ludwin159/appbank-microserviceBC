package com.bank.appbank.controller;

import com.bank.appbank.model.Credit;
import com.bank.appbank.model.CreditCard;
import com.bank.appbank.service.CreditCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@WebFluxTest(CreditCardController.class)
class CreditCardControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CreditCardService creditCardService;

    @Test
    void update() {
        CreditCard creditCard1 = new CreditCard();
        creditCard1.setId("CREDIT_CARD001");
        creditCard1.setIdClient("clientN001");
        creditCard1.setLimitCredit(1000.0);
        creditCard1.setAvailableBalance(500.0);
        creditCard1.setNumberDueDate("5");
        creditCard1.setNumberBillingDate("20");
        creditCard1.setTotalDebt(500.0);

        when(creditCardService.update(eq(creditCard1.getId()), any())).thenReturn(Mono.just(creditCard1));

        webTestClient.put().uri("/credit-cards/CREDIT_CARD001")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(creditCard1)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("CREDIT_CARD001");
    }
}