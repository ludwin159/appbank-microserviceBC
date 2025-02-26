package com.bank.appbank.client;

import com.bank.appbank.dto.ConsumptionDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class ConsumptionServiceClient {
    private final WebClient webClient;
    public ConsumptionServiceClient(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://movementsmicroservice").build();
    }

    public Flux<ConsumptionDto> findAllConsumptionsByIdCreditCardAndSortByDate(String idCreditCard) {
        return webClient.get().uri("/consumptions/getAllByIdCreditCard/{idCreditCard}", idCreditCard)
                .retrieve()
                .bodyToFlux(ConsumptionDto.class);
    }

    public Flux<ConsumptionDto> findByIdCreditCardAndBilledFalse(String idCreditCard) {
        return webClient.get().uri("/consumptions/findByIdCreditCardAndBilledFalse/{idCreditCard}", idCreditCard)
                .retrieve()
                .bodyToFlux(ConsumptionDto.class);
    }
    public Flux<ConsumptionDto> saveAll(List<ConsumptionDto> consumptions) {
        return webClient.post().uri("/consumptions/saveAll")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(consumptions))
                .retrieve()
                .bodyToFlux(ConsumptionDto.class);
    }
}
