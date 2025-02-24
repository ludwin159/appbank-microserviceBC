package com.bank.appbank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class ConsumptionDto {
    private String id;
    private String idCreditCard;
    private Double amount;
    private LocalDateTime dateConsumption;
    private String description;

    public ConsumptionDto() {
        this.dateConsumption = LocalDateTime.now();
    }
}
