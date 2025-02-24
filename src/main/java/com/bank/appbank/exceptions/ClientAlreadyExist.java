package com.bank.appbank.exceptions;

public class ClientAlreadyExist extends RuntimeException {
    public ClientAlreadyExist(String message) {
        super(message);
    }
}
