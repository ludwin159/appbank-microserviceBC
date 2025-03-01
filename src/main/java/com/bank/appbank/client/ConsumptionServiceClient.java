package com.bank.appbank.client;

import com.bank.appbank.dto.ConsumptionDto;
import com.bank.appbank.exceptions.ServiceNotAvailableException;
import com.bank.appbank.exceptions.UnsupportedMovementException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ConsumptionServiceClient {
    private final WebClient webClient;
    private final static Logger log = LoggerFactory.getLogger(ConsumptionServiceClient.class);
    private final String serviceNotAvailableMessage = "The endpoint of consumptions is not available";
    public ConsumptionServiceClient(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://movementsmicroservice").build();
    }

    @CircuitBreaker(name = "consumptionCircuitBreaker", fallbackMethod = "fallbackFindAllConsumption")
    @TimeLimiter(name = "consumptionCircuitBreaker")
    public Flux<ConsumptionDto> findAllConsumptionsByIdCreditCardAndSortByDate(String idCreditCard) {
        return webClient.get().uri("/consumptions/getAllByIdCreditCard/{idCreditCard}", idCreditCard)
                .retrieve()
                .bodyToFlux(ConsumptionDto.class);
    }

    @CircuitBreaker(name = "consumptionCircuitBreaker", fallbackMethod = "fallbackNotBilledFalse")
    @TimeLimiter(name = "consumptionCircuitBreaker")
    public Flux<ConsumptionDto> findByIdCreditCardAndBilledFalse(String idCreditCard) {
        return webClient.get().uri("/consumptions/findByIdCreditCardAndBilledFalse/{idCreditCard}", idCreditCard)
                .retrieve()
                .bodyToFlux(ConsumptionDto.class);
    }

    @CircuitBreaker(name = "consumptionCircuitBreaker", fallbackMethod = "fallbackSaveAll")
    @TimeLimiter(name = "consumptionCircuitBreaker")
    public Flux<ConsumptionDto> saveAll(List<ConsumptionDto> consumptions) {
        return webClient.post().uri("/consumptions/saveAll")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(consumptions))
                .retrieve()
                .bodyToFlux(ConsumptionDto.class);
    }

    @CircuitBreaker(name = "consumptionCircuitBreaker", fallbackMethod = "fallbackLastConsumptions")
    @TimeLimiter(name = "consumptionCircuitBreaker")
    public Mono<List<ConsumptionDto>> findLastTenConsumptions(List<String> idConsumptions) {
        log.info("Pasa por ajuya" + idConsumptions);
        return webClient.post().uri("/consumptions/find-last-ten-credit-card")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(idConsumptions)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ConsumptionDto>>() {});
    }

    public Flux<String> fallbackFindAllConsumption(String idCreditCard, Throwable error) {
        return Flux.error(new ServiceNotAvailableException(serviceNotAvailableMessage));
    }
    public Flux<String> fallbackNotBilledFalse(String idCreditCard, Throwable error) {
        return Flux.error(new ServiceNotAvailableException(serviceNotAvailableMessage));
    }
    public Flux<String> fallbackSaveAll(List<ConsumptionDto> consumptions, Throwable error) {
        return Flux.error(new ServiceNotAvailableException(serviceNotAvailableMessage));
    }
    public Mono<String> fallbackLastConsumptions(List<String> idConsumptions, Throwable error) {
        log.error("Tiene un error por ajuya" + idConsumptions);
        return Mono.error(new UnsupportedMovementException(serviceNotAvailableMessage));
    }
}
