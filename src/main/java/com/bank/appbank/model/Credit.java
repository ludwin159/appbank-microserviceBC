package com.bank.appbank.model;

import com.bank.appbank.dto.PaymentDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Document(collection = "credits")
public class Credit extends BankProduct {
    @NotNull
    @Min(0)
    private Double totalAmount;

    @NotNull
    @Min(0)
    private Double pendingBalance;

    @NotNull
    @Min(0)
    private Double interestRate;

    @Transient
    private List<PaymentDto> payments;

    public Credit(String idClient, Double totalAmount, Double pendingBalance, Double interestRate) {
        super(idClient);
        this.totalAmount = totalAmount;
        this.pendingBalance = pendingBalance;
        this.interestRate = interestRate;
        this.payments = new ArrayList<>();
    }
}
