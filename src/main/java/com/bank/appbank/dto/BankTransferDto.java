package com.bank.appbank.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BankTransferDto {
    private String id;
    private LocalDateTime date;
    private Double amount;
    private String idBankAccountFrom;
    private String idBankAccountTo;
    private String description;

    public BankTransferDto() {
        this.date = LocalDateTime.now();
    }
}
