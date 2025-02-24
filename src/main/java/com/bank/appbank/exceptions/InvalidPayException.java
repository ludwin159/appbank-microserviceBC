package com.bank.appbank.exceptions;

public class InvalidPayException extends RuntimeException {
    public InvalidPayException(String message) {
        super(message);
    }
}
