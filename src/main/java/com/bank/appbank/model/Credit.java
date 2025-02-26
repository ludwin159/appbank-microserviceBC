package com.bank.appbank.model;

import com.bank.appbank.dto.PaymentDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cglib.core.Local;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
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
    @Max(1)
    private Double interestRate;

    @NotNull
    private LocalDate disbursementDate;

    @NotNull
    private LocalDate firstDatePay;

    @NotNull
    @Min(0)
    private Integer totalMonths;

    private Double monthlyFee;
    @CreatedDate
    private LocalDateTime createdAt;

    @Transient
    private List<PaymentDto> payments;


    public Credit(String idClient,
                  Double totalAmount,
                  Double pendingBalance,
                  Double interestRate,
                  LocalDate disbursementDate,
                  LocalDate firstDatePay,
                  Integer totalMonths,
                  Double monthlyFee) {
        super(idClient);
        this.totalAmount = totalAmount;
        this.pendingBalance = pendingBalance;
        this.interestRate = interestRate;
        this.disbursementDate = disbursementDate;
        this.firstDatePay = firstDatePay;
        this.totalMonths = totalMonths;
        this.monthlyFee = monthlyFee;
        this.payments = new ArrayList<>();
    }
}
