package com.bank.appbank.model;

import com.bank.appbank.dto.ConsumptionDto;
import com.bank.appbank.dto.PaymentDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;


@Setter
@Getter
@Document(collection = "credit_cards")
public class CreditCard extends BankProduct {

    @NotNull
    @Min(0)
    private Double limitCredit;

    @NotNull
    @Min(0)
    private Double availableBalance;

    @NotNull
    @Min(0)
    @Max(1)
    private Double interestRate;
    @Transient
    private List<ConsumptionDto> consumptions;
    @Transient
    private List<PaymentDto> payments;

    public CreditCard(String idClient) {
        super(idClient);
        this.consumptions = new ArrayList<>();
        this.payments = new ArrayList<>();
    }

    public CreditCard() {
        this.consumptions = new ArrayList<>();
        this.payments = new ArrayList<>();
    }
}
