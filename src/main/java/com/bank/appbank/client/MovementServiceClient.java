package com.bank.appbank.client;

import com.bank.appbank.dto.MovementDto;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
public class MovementServiceClient {

    private final WebClient webClient;
    public MovementServiceClient(WebClient.Builder webClient) {
        this.webClient = webClient.baseUrl("http://movementsmicroservice").build();
    }
    public Flux<MovementDto> getMovementsByBankAccountIdInPresentMonth(String idBankAccount) {
        return webClient.get()
                .uri("/movements/getAllByIdBankAccountInPresentMonth/{idBankAccount}", idBankAccount)
                .retrieve()
                .bodyToFlux(MovementDto.class);
    }

    public Flux<MovementDto> getAllMovementsByIdBankAccountAndSortByDate(String idBankAccount) {
        return webClient.get()
                .uri("/movements/getAllByIdBankAccount/{idBankAccount}", idBankAccount)
                .retrieve()
                .bodyToFlux(MovementDto.class);
    }

    public Flux<MovementDto> getAllMovementsByRangeDate(String from, String to) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/movements/getAllByRangeDate")
                        .queryParam("from", from)
                        .queryParam("to", to).build())
                .retrieve()
                .bodyToFlux(MovementDto.class);
    }
}
