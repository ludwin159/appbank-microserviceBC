package com.bank.appbank.controller;

import com.bank.appbank.model.CreditCard;
import com.bank.appbank.service.CreditCardService;
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
@RequestMapping("/credit-cards")
@Tag(name = "Tarjetas de Crédito", description = "Gestiona las tarjetas de crédito de los clientes.")
public class CreditCardController extends ControllerT<CreditCard, String> {

    private final CreditCardService creditCardService;

    public CreditCardController(CreditCardService creditCardService) {
        super(creditCardService);
        this.creditCardService = creditCardService;
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una tarjeta de crédito",
            description = "Actualiza la información de una tarjeta de crédito existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tarjeta de crédito actualizada",
                    content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = CreditCard.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Tarjeta de crédito no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<CreditCard> update(@PathVariable String id, @Valid @RequestBody CreditCard currentAccount) {
        return creditCardService.update(id, currentAccount);
    }

    @GetMapping("/findAllByIdClientWithMovements/{idClient}")
    @Operation(summary = "Obtener tarjetas de crédito de un cliente con pagos y consumos",
            description = "Retorna todas las tarjetas de crédito de un cliente, incluyendo sus pagos y consumos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación exitosa",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CreditCard.class))),
            @ApiResponse(responseCode = "404", description = "Cliente o tarjetas de crédito no encontradas"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Flux<CreditCard> allCreditCardsByIdClientWithPaymentAndConsumption(@PathVariable String idClient) {
        return creditCardService.allCreditCardsByIdClientWithPaymentAndConsumption(idClient);
    }
}
