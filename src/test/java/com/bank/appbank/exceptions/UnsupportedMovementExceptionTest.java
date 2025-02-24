package com.bank.appbank.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnsupportedMovementExceptionTest {

    @Test
    @DisplayName("When a movement type is not valid")
    void UnsupportedMovement() {
        String errorMessage = "The movement type is not valid.";
        UnsupportedMovementException exception = assertThrows(UnsupportedMovementException.class, () -> {
            throw new UnsupportedMovementException(String.format(errorMessage));
        });
        assertEquals(errorMessage, exception.getMessage());
    }
}