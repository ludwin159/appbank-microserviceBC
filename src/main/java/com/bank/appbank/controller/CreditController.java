package com.bank.appbank.controller;

import com.bank.appbank.model.Credit;
import com.bank.appbank.service.CreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;


@RestController
@RequestMapping("/credits")
@Slf4j
@Tag(name = "Créditos", description = "Gestiona los créditos de los clientes.")
public class CreditController extends ControllerT<Credit, String> {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        super(creditService);
        this.creditService = creditService;
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un crédito", description = "Actualiza la información de un crédito existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Crédito actualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Credit.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Crédito no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Credit> update(@Parameter(description = "ID del crédito") @PathVariable String id,
                               @Valid @RequestBody Credit credit) {
        return creditService.update(id, credit);
    }

    @GetMapping("/findAllByClientWithPayments/{idClient}")
    @Operation(summary = "Obtener créditos de un cliente con pagos",
            description = "Retorna todos los créditos de un cliente, incluyendo sus pagos ordenados por fecha de pago.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Credit.class))),
            @ApiResponse(responseCode = "404", description = "Cliente o créditos no encontrados"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Flux<Credit> allCreditsByIdClientWithAllPaymentsSortedByDatePayment(
            @Parameter(description = "ID del cliente") @PathVariable String idClient) {
        return creditService.allCreditsByIdClientWithAllPaymentsSortedByDatePayment(idClient);
    }
}
