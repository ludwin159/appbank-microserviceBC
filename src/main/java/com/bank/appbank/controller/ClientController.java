package com.bank.appbank.controller;

import com.bank.appbank.model.Client;
import com.bank.appbank.service.ClientService;
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
@RequestMapping("/client")
@Tag(name = "Clientes", description = "Gestiona los clientes para la aplicación.")
public class ClientController extends ControllerT<Client, String> {
    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        super(clientService);
        this.clientService = clientService;
    }
    @Override
    @PostMapping
    @Operation(summary = "Crear un nuevo cliente",
            description = "Crea un nuevo cliente.  Las reglas de unicidad "+
                    "dependen del tipo de cliente (personal o empresarial).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente creado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Client.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o cliente ya existe"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Client> create(@Valid @RequestBody Client client) {
        return super.create(client);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un cliente", description = "Actualiza la información de un cliente existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente actualizado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Client.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public Mono<Client> update(@PathVariable String id, @Valid @RequestBody Client client) {
        return clientService.updateClient(id, client);
    }
}
