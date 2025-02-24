package com.bank.appbank.exceptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientNotFoundExceptionTest {

    @Test
    void testClientNotFoundException() {
        String idNotFound = "458965235s9568s";
        String errorMessage = "The client with id: " + idNotFound + " does not exit.";
        ClientNotFoundException exception = assertThrows(ClientNotFoundException.class, () -> {
           throw new ClientNotFoundException(errorMessage);
        });
        assertEquals(errorMessage, exception.getMessage());
    }

}