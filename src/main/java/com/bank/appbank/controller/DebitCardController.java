package com.bank.appbank.controller;

import com.bank.appbank.model.DebitCard;
import com.bank.appbank.service.DebitCardService;
import com.bank.appbank.service.ServiceT;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/debit-cards")
public class DebitCardController extends ControllerT<DebitCard, String>{
    private DebitCardService debitCardService;
    public DebitCardController(DebitCardService debitCardService) {
        super(debitCardService);
    }

    @PutMapping("/{idDebitCard}")
    public Mono<DebitCard> update(@PathVariable String idDebitCard, @Valid @RequestBody DebitCard debitCard) {
        return debitCardService.update(idDebitCard, debitCard);
    }
}
