package com.bank.appbank.client;

import com.bank.appbank.dto.ConsumptionDto;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

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
}
