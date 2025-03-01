package com.bank.appbank.client;

import com.bank.appbank.dto.MovementDto;
import com.bank.appbank.exceptions.ServiceNotAvailableException;
import com.bank.appbank.exceptions.UnsupportedMovementException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class MovementServiceClient {

    private final WebClient webClient;
    private final static Logger log = LoggerFactory.getLogger(MovementServiceClient.class);
    private String errorMessage = "The backend for movements is not available";
    public MovementServiceClient(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://movementsmicroservice").build();
    }
    @CircuitBreaker(name = "movementsCircuitBreaker", fallbackMethod = "fallbackMovementsByBankAccount")
    @TimeLimiter(name = "movementsCircuitBreaker")
    public Flux<MovementDto> getMovementsByBankAccountIdInPresentMonth(String idBankAccount) {
        return webClient.get()
                .uri("/movements/getAllByIdBankAccountInPresentMonth/{idBankAccount}", idBankAccount)
                .retrieve()
                .bodyToFlux(MovementDto.class);
    }

    @CircuitBreaker(name = "movementsCircuitBreaker", fallbackMethod = "fallbackAllMovementsByBankAccount")
    @TimeLimiter(name = "movementsCircuitBreaker")
    public Flux<MovementDto> getAllMovementsByIdBankAccountAndSortByDate(String idBankAccount) {
        return webClient.get()
                .uri("/movements/getAllByIdBankAccount/{idBankAccount}", idBankAccount)
                .retrieve()
                .bodyToFlux(MovementDto.class);
    }

    @CircuitBreaker(name = "movementsCircuitBreaker", fallbackMethod = "fallbackAllMovementByRangeDate")
    @TimeLimiter(name = "movementsCircuitBreaker")
    public Flux<MovementDto> getAllMovementsByRangeDate(String from, String to) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/movements/getAllByRangeDate")
                        .queryParam("from", from)
                        .queryParam("to", to).build())
                .retrieve()
                .bodyToFlux(MovementDto.class);
    }
    @CircuitBreaker(name = "movementsCircuitBreaker", fallbackMethod = "fallbackLastTenMovements")
    @TimeLimiter(name = "movementsCircuitBreaker")
    public Mono<List<MovementDto>> getLastTenMovements(List<String> idBankAccounts) {
        log.info("Get last then movements from: " + idBankAccounts);
        return webClient.post().uri("/movements/last-ten-by-bank-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(idBankAccounts)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<MovementDto>>() {});

    }

    public Flux<MovementDto> fallbackMovementsByBankAccount(String idBankAccount, Throwable ex) {
        return Flux.error(new ServiceNotAvailableException(errorMessage));
    }
    public Flux<MovementDto> fallbackAllMovementsByBankAccount(String idBankAccount, Throwable ex) {
        return Flux.error(new ServiceNotAvailableException(errorMessage));
    }

    public Flux<MovementDto> fallbackAllMovementByRangeDate(String from, String to, Throwable ex) {
        errorMessage = "Could not bring movements in the date range";
        return Flux.error(new ServiceNotAvailableException(errorMessage));
    }
    public Mono<MovementDto> fallbackLastTenMovements(List<String> idBankAccounts, Throwable ex) {
        errorMessage = "Could not bring the last 10 movements";
        log.error("ERROR fallback last ten movements");
        return Mono.error(new ServiceNotAvailableException(errorMessage));
    }


}
