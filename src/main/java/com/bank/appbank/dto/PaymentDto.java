package com.bank.appbank.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PaymentDto {
    private String id;
    private Double amount;
    private String idProductCredit;
    private LocalDateTime datePayment;
    private TypeCreditProduct typeCreditProduct;
    private Integer monthCorresponding;
    private Integer yearCorresponding;
    private Double penaltyFee;
    private LocalDateTime createdAt;

    public PaymentDto() {
        this.datePayment = LocalDateTime.now();
    }
    public enum TypeCreditProduct {
        CREDIT_CARD, CREDIT, UNSUPPORTED
    }
}
