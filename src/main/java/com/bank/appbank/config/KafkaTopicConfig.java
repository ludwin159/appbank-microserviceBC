package com.bank.appbank.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic associateTopic() {
        return TopicBuilder.name("wallet-debit-card-association")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic responseTopic() {
        return TopicBuilder.name("wallet-debit-card-response")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bankAccountBalanceUpdated() {
        return TopicBuilder.name("bank-account-balance-updated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic makePaymentWithDebitCard() {
        return TopicBuilder.name("payment-with-debit-card")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentConfirmation() {
        return TopicBuilder.name("payment-with-debit-card-confirmation")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
