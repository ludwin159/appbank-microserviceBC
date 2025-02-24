package com.bank.appbank.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("When a anything is not present")
    void resourceNotFound() {
        String errorMessage = "The client not exists in the database.";
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            throw new ResourceNotFoundException(String.format(errorMessage));
        });
        assertEquals(errorMessage, exception.getMessage());
    }
}