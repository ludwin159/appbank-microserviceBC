package com.bank.appbank.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LimitMovementsExceededTest {

    @Test
    @DisplayName("When a client exceeded its limit movements")
    void limitMovementsExceeded() {
        String errorMessage = "The client has not enough balance.";
        LimitMovementsExceeded exception = assertThrows(LimitMovementsExceeded.class, () -> {
            throw new LimitMovementsExceeded(String.format(errorMessage));
        });
        assertEquals(errorMessage, exception.getMessage());
    }

}