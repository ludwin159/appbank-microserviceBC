package com.bank.appbank.exceptions;

public class UnsupportedMovementException extends RuntimeException {
    public UnsupportedMovementException(String message) {
        super(message);
    }
}
