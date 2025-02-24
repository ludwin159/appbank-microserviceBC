package com.bank.appbank.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Data
@Document(collection = "daily_balances")
public class DailyBalance {
    @Id
    private String id;
    @Indexed
    private String idBankProduct;
    @Indexed
    private LocalDate date;
    private Double balance;
}
