package com.bank.appbank.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LimitAccountsExceptionTest {

    @Test
    @DisplayName("When a client of type personal has a accounts limit")
    public void limitAccountsException() {

        String errorMessage = "The client does not have more accounts of this type.";
        LimitAccountsException exception = assertThrows(LimitAccountsException.class, () -> {
            throw new LimitAccountsException(String.format(errorMessage));
        });
        assertEquals(errorMessage, exception.getMessage());
    }
}