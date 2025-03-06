package com.bank.appbank.event.consumer;

import com.bank.appbank.model.MovementWallet;
import com.bank.appbank.dto.ResponseAssociationWalletDto;
import com.bank.appbank.service.DebitCardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DebitCardConsumer {

    private static final Logger log = LoggerFactory.getLogger(DebitCardConsumer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DebitCardService debitCardService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DebitCardConsumer(KafkaTemplate<String, String> kafkaTemplate,
                             DebitCardService debitCardService) {
        this.kafkaTemplate = kafkaTemplate;
        this.debitCardService = debitCardService;
    }

    @KafkaListener(topics = "wallet-debit-card-association", groupId = "appbank-group")
    public void listenAssociationRequest(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String idWallet = json.get("idWallet").asText();
            String idDebitCard = json.get("idDebitCard").asText();

            log.info("Association received: " + message);
            debitCardService.validAssociationWalletToDebitCard(idWallet, idDebitCard)
                    .doOnError(error -> {
                        ResponseAssociationWalletDto response = ResponseAssociationWalletDto.builder()
                                .idWallet(idWallet)
                                .idDebitCard(idDebitCard)
                                .state("REJECTED")
                                .balance(0.0)
                                .idMessage(UUID.randomUUID().toString())
                                .observation(error.getMessage()).build();
                        kafkaTemplate.send("wallet-debit-card-response", serializeMessage(response));

                    })
                    .doOnSuccess(response -> {
                        response.setIdMessage(UUID.randomUUID().toString());
                        log.info("Valid association: " + idWallet + "-" + idDebitCard);
                        kafkaTemplate.send("wallet-debit-card-response", serializeMessage(response));
                    })
                    .subscribe();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "payment-with-debit-card", groupId = "appbank-group")
    public void listenerPaymentWithDebitCard(String message) {
        try {
            List<MovementWallet> movements = objectMapper.readValue(
                    message, new TypeReference<List<MovementWallet>>() {});
            debitCardService.paymentWalletWithDebitCard(movements)
                    .subscribe();
        } catch (Exception e) {
            log.error("Error deserializing message in Payment wallet with debit card: {}", e.getMessage(), e);
        }
    }


    public String serializeMessage(Object message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("ERROR in serialized json: " + e.getMessage());
            throw new RuntimeException("ERROR in serialized json: " + e.getMessage());
        }
    }


}
