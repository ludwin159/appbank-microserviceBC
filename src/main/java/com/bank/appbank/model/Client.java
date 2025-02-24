package com.bank.appbank.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@Document(collection = "clients")
public class Client {
    @Id
    private String id;

    @NotNull
    @Indexed
    private String identity;

    @NotNull
    private String fullName;

    @NotNull
    @Indexed
    private String taxId;

    @NotNull
    private String businessName;

    @NotNull
    private String address;

    @NotNull
    private String phone;

    @Pattern(regexp = "^[^@]+@[^@]+\\.[a-zA-Z]{2,}$", message = "Input a valid email")
    private String email;
    private TypeClient typeClient;

    public enum TypeClient {
        BUSINESS_CLIENT, PERSONAL_CLIENT, BUSINESS_PYMES_CLIENT, PERSONAL_VIP_CLIENT
    }
}
