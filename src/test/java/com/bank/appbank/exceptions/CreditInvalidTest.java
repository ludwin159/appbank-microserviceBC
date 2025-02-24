package com.bank.appbank.exceptions;

import com.bank.appbank.model.Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreditInvalidTest {

    @Test
    @DisplayName("When a customer of type PERSONAL has a credit.")
    void creditInvalid() {
        String idClient = "458965235s9568s";
        Client.TypeClient typeClient = Client.TypeClient.PERSONAL_CLIENT;
        String messageError = String.format("The customer %s of type: %s, already has a credit.", idClient, typeClient);
        CreditInvalid exception = assertThrows(CreditInvalid.class, () -> {
           throw new CreditInvalid(messageError);
        });
        assertEquals(messageError, exception.getMessage());
    }

}