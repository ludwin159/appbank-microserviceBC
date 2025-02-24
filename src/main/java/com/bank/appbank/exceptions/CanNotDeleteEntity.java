package com.bank.appbank.exceptions;

public class CanNotDeleteEntity extends RuntimeException {
    public CanNotDeleteEntity(String message) {
        super(message);
    }
}
