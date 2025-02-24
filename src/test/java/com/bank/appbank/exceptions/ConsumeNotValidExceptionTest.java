package com.bank.appbank.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsumeNotValidExceptionTest {

    @Test
    @DisplayName("Valid throw an exception when a consume is not valid")
    void consumeNotValidException() {
        String errorMessage = "Consumption is greater than the available balance.";
        ConsumeNotValidException exception = assertThrows(ConsumeNotValidException.class, () -> {
            throw new ConsumeNotValidException(errorMessage);
        });
        assertEquals(errorMessage, exception.getMessage());

    }

}