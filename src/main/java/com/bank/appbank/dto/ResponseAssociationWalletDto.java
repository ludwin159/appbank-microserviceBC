package com.bank.appbank.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class ResponseAssociationWalletDto {
    private String idDebitCard;
    private String idWallet;
    private String observation;
    private String state;
    private Double balance;
    private String idMessage;
}
