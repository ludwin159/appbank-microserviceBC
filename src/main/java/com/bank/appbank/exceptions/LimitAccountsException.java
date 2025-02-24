package com.bank.appbank.exceptions;

public class LimitAccountsException extends RuntimeException {
    public LimitAccountsException(String message) {
        super(message);
    }
}
