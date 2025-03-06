package com.bank.appbank.event.producer;


import com.bank.appbank.model.MovementWallet;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DebitCardProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    public DebitCardProducer(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishConfirmationPaymentDebit(List<MovementWallet> movementsWallet) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(movementsWallet);
            kafkaTemplate.send("payment-with-debit-card-confirmation", jsonMessage);
            log.info("Movement with debit card confirmation: {}", jsonMessage);
        } catch (Exception e) {
            log.error("Error serializing confirmation payment debit with card", e);
        }
    }
}
