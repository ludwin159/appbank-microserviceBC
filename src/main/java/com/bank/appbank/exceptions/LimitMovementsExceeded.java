package com.bank.appbank.exceptions;

public class LimitMovementsExceeded extends RuntimeException {
    public LimitMovementsExceeded(String message) {
        super(message);
    }
}
