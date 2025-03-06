package com.bank.appbank.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class BankAccountProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public BankAccountProducer(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishBalanceUpdate(String idDebitCard, double newBalance) {
        try {
            Map<String, Object> message = Map.of(
                    "idDebitCard", idDebitCard,
                    "newBalance", newBalance
            );
            String jsonMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send("bank-account-balance-updated", jsonMessage);
            log.info("Balance update event sent: {}", jsonMessage);
        } catch (Exception e) {
            log.error("Error serializing balance update event", e);
        }
    }

}
