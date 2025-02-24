package com.bank.appbank.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InsufficientBalanceTest {

    @Test
    @DisplayName("When a client has not enough balance")
    void invalidBalance() {
        String errorMessage = "The client has not enough balance.";
        InsufficientBalance exception = assertThrows(InsufficientBalance.class, () -> {
            throw new InsufficientBalance(String.format(errorMessage));
        });
        assertEquals(errorMessage, exception.getMessage());
    }
}