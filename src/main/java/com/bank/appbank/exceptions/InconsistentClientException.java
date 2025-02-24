package com.bank.appbank.exceptions;

public class InconsistentClientException extends RuntimeException{
    public InconsistentClientException(String message) {
        super(message);
    }
}
