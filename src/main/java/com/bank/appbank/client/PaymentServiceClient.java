package com.bank.appbank.client;

import com.bank.appbank.dto.PaymentDto;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
public class PaymentServiceClient {

    private WebClient webClient;

    public PaymentServiceClient(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://movementsmicroservice").build();
    }

    public Flux<PaymentDto> findAllPaymentByIdProductCreditAndSortByDate(String idProductCredit) {
        return webClient.get().uri("/payments/getAllByIdProductCredit/{idProductCredit}", idProductCredit)
                .retrieve()
                .bodyToFlux(PaymentDto.class);
    }
}
