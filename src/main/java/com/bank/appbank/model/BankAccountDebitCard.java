package com.bank.appbank.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "bank_accounts_debit_cards")
public class BankAccountDebitCard {
    private String id;
    @CreatedDate
    private LocalDateTime createdAt;

    @NotNull
    @NotBlank
    @Size(min = 8)
    private String idBankAccount;
    @NotNull
    @NotBlank
    @Size(min = 8)
    private String idDebitCard;
}
