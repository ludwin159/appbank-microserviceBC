package com.bank.appbank.exceptions;

public class CreditInvalid extends RuntimeException {
    public CreditInvalid(String message) {
        super(message);
    }
}
