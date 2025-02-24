package com.bank.appbank.exceptions;

public class IneligibleClientException extends RuntimeException {
    public IneligibleClientException(String message) {
        super(message);
    }
}
