package com.bank.appbank.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InvalidPayExceptionTest {

    @Test
    @DisplayName("When a pay is invalid for different reasons")
    void invalidBalance() {
        String errorMessage = "The pay is invalid for some reasons.";
        InvalidPayException exception = assertThrows(InvalidPayException.class, () -> {
            throw new InvalidPayException(String.format(errorMessage));
        });
        assertEquals(errorMessage, exception.getMessage());
    }
}