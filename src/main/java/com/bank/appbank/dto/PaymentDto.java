package com.bank.appbank.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentDto {
    private String id;
    private Double amount;
    private String idProductCredit;
    private LocalDateTime datePayment;
    private TypeCreditProduct typeCreditProduct;

    public PaymentDto() {
        this.datePayment = LocalDateTime.now();
    }
    public enum TypeCreditProduct {
        CREDIT_CARD, CREDIT, UNSUPPORTED
    }
}
