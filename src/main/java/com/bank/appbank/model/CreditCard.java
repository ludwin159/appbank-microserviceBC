package com.bank.appbank.model;

import com.bank.appbank.dto.ConsumptionDto;
import com.bank.appbank.dto.PaymentDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
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
    private Double totalDebt;

    @NotNull
    @Pattern(regexp = "^(20|13)$", message = "The billing date can be 20 or 13")
    private String numberBillingDate;

    @NotNull
    @Pattern(regexp = "^(5|28)$", message = "The number due date can be 5 or 28")
    private String numberDueDate;

    private LocalDate dueDate;

    @CreatedDate
    private LocalDateTime createdAt;

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
