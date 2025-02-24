package com.bank.appbank.controller;

import com.bank.appbank.model.BankAccount;
import com.bank.appbank.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/bank-accounts")
@Tag(name = "Cuentas Bancarias", description = "Gestiona las cuentas bancarias de los clientes.")
public class BankAccountController extends ControllerT<BankAccount, String>{

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        super(bankAccountService);
        this.bankAccountService = bankAccountService;
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una cuenta bancaria",
            description = "Actualiza la información de una cuenta bancaria existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cuenta bancaria actualizada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BankAccount.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Cuenta bancaria no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<BankAccount> update(@PathVariable String id, @Valid @RequestBody BankAccount bankAccount) {
        return bankAccountService.update(id, bankAccount);
    }

    @GetMapping("/findAllByClientWithMovements/{idClient}")
    @Operation(summary = "Obtener cuentas bancarias de un cliente con movimientos",
            description = "Retorna todas las cuentas bancarias de un cliente, "
                    +"incluyendo sus movimientos ordenados por fecha.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BankAccount.class))),
            @ApiResponse(responseCode = "404", description = "Cliente o cuentas bancarias no encontradas"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Flux<BankAccount> findBankAccountsByIdClientWithAllMovementsSortedByDate(@PathVariable String idClient) {
        return bankAccountService.findBankAccountsByIdClientWithAllMovementsSortedByDate(idClient);
    }
}
