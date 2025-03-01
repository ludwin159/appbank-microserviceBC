package com.bank.appbank.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MovementDto {
    private String id;
    private LocalDateTime date;
    private TypeMovement typeMovement;
    private Double amount;
    private String description;
    private String idBankAccount;
    private Double commissionAmount;
    private String idBankAccountTransfer;
    private String idTransfer;
    private LocalDateTime createdAt;

    public MovementDto() {
        this.date = LocalDateTime.now();
    }
    public static enum TypeMovement {
        DEPOSIT, WITHDRAWAL, TRANSFER, UNSUPPORTED, PAY_CREDIT, WITHDRAWAL_DEBIT
    }
}
