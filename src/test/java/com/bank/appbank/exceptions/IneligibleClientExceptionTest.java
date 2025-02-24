package com.bank.appbank.exceptions;

import com.bank.appbank.model.Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IneligibleClientExceptionTest {

    @Test
    @DisplayName("When a client of type PERSONAL only have a current account")
    void ineligibleClient() {
        String idClient = "458965235s9568s";
        Client.TypeClient typeClient = Client.TypeClient.PERSONAL_CLIENT;
        String errorMessage = "The client: %s of type: %s only can have current accounts.";
        IneligibleClientException exception = assertThrows(IneligibleClientException.class, () -> {
            throw new IneligibleClientException(String.format(errorMessage, idClient, typeClient));
        });
        assertEquals(String.format(errorMessage, idClient, typeClient), exception.getMessage());
    }

}