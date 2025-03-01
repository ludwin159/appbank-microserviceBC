package com.bank.appbank.client;

import com.bank.appbank.dto.PaymentDto;
import com.bank.appbank.exceptions.ServiceNotAvailableException;
import com.bank.appbank.exceptions.UnsupportedMovementException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class PaymentServiceClient {

    private final String messageError = "The payment service is not available.";
    private final static Logger log = LoggerFactory.getLogger(PaymentServiceClient.class);
    private final WebClient webClient;

    public PaymentServiceClient(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://movementsmicroservice").build();
    }

    @CircuitBreaker(name = "paymentsCircuitBreaker", fallbackMethod = "fallbackFindAllPaymentByIdProduct")
    @TimeLimiter(name = "paymentsCircuitBreaker")
    public Flux<PaymentDto> findAllPaymentByIdProductCreditAndSortByDate(String idProductCredit) {
        return webClient.get().uri("/payments/getAllByIdProductCredit/{idProductCredit}", idProductCredit)
                .retrieve()
                .bodyToFlux(PaymentDto.class);
    }
    @CircuitBreaker(name = "paymentsCircuitBreaker", fallbackMethod = "fallbackLastTenPayments")
    @TimeLimiter(name = "paymentsCircuitBreaker")
    public Mono<List<PaymentDto>> lastTenPaymentsByIdCreditCard(List<String> idCreditCards) {
        log.info("Pasa por aqui");
        return webClient.post().uri("/payments/get-last-ten-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(idCreditCards)
                .retrieve()
                .onStatus(HttpStatus::isError, error -> {
                    System.out.println(error);
                    return Mono.just(new RuntimeException(error.toString()));
                })
                .bodyToMono(new ParameterizedTypeReference<List<PaymentDto>>() {});
    }

    public Flux<String> fallbackFindAllPaymentByIdProduct(String idProductCredit, Throwable error) {
        return Flux.error(new ServiceNotAvailableException(messageError));
    }

    public Mono<String> fallbackLastTenPayments(List<String> idCreditCards, Throwable error) {
        log.error("Error capturado: " + idCreditCards);
        return Mono.error(new ServiceNotAvailableException(messageError));
    }
}
