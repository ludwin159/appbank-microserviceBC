package com.bank.appbank.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;


@Getter
@Setter
@NoArgsConstructor
public abstract class BankProduct {
    @Id
    protected String id;
    @Indexed
    protected String idClient;

    public BankProduct(String idClient) {
        this.idClient = idClient;
    }
}
