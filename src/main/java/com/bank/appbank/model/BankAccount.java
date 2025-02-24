package com.bank.appbank.model;

import com.bank.appbank.dto.BankTransferDto;
import com.bank.appbank.dto.MovementDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Document(collection = "bank_accounts")
public class BankAccount extends BankProduct {

    @NotNull
    @Min(value = 0, message = "The minimum amount to open an account is zero.")
    private Double balance;
    private TypeBankAccount typeBankAccount;

    @NotNull
    @Min(value = 0)
    private Integer limitMovements; // Not more movements

    @NotNull
    @Min(value = 1)
    private Integer maxTransactions; // more with commission

    @NotNull
    @Min(value = 0)
    @Max(value = 1)
    private Double commissionPercentage;

    @NotNull
    @Min(value = 0)
    private Integer expirationDate;

    @NotNull
    @Min(value = 0)
    private Double maintenanceCost;

    @NotNull
    @Min(value = 0)
    private Double minimumDailyAverageAmount;

    private List<String> authorizedSignatorits;
    private List<String> accountHolders;
    @Transient
    private List<MovementDto> movements;

    public BankAccount() {
        movements = new ArrayList<>();
    }

    public BankAccount(String idClient,
                       Double balance,
                       TypeBankAccount typeBankAccount,
                       Integer limitMovements,
                       Integer expirationDate,
                       Double maintenanceCost,
                       Double commissionPercentage,
                       Double minimumDailyAverageAmount,
                       Integer maxTransactions,
                       List<String> authorizedSignatorits,
                       List<String> accountHolders) {
        super(idClient);
        this.balance = balance;
        this.typeBankAccount = typeBankAccount;
        this.limitMovements = limitMovements;
        this.expirationDate = expirationDate;
        this.maintenanceCost = maintenanceCost;
        this.minimumDailyAverageAmount = minimumDailyAverageAmount;
        this.commissionPercentage = commissionPercentage;
        this.maxTransactions = maxTransactions;
        this.authorizedSignatorits = authorizedSignatorits;
        this.accountHolders = accountHolders;
        this.movements = new ArrayList<>();
    }

    public enum TypeBankAccount {
        SAVING_ACCOUNT, CURRENT_ACCOUNT, FIXED_TERM_ACCOUNT
    }
}
