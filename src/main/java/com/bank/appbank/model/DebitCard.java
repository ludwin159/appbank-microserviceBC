package com.bank.appbank.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Document(collection = "debit_cars")
public class DebitCard {
    @Id
    private String id;
    @NotNull
    @NotBlank
    private String idClient;
    @NotNull
    @Size(min = 8)
    private String idPrincipalAccount;

    @NotNull
    @Size(min = 8)
    private String numberCard;

    @Transient
    private List<BankAccount> bankAccounts;
    public DebitCard() {
        this.bankAccounts = new ArrayList<>();
    }
}
