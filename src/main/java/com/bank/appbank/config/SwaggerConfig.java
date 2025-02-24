package com.bank.appbank.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "API's for microservice Bank Application",
                version = "v1.0.0",
                description = "In this microservice you can management your bank products",
                contact = @Contact(
                        name = "Ludwin J. Suárez",
                        email = "lsuarein@emeal.nttdata.com"
                )
        )
)
public class SwaggerConfig {}
