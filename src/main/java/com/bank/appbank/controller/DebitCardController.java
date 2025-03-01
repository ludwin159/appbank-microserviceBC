package com.bank.appbank.controller;

import com.bank.appbank.model.BankAccountDebitCard;
import com.bank.appbank.model.DebitCard;
import com.bank.appbank.service.DebitCardService;
import com.bank.appbank.service.ServiceT;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debit-cards")
public class DebitCardController extends ControllerT<DebitCard, String>{
    private final DebitCardService debitCardService;
    public DebitCardController(DebitCardService debitCardService) {
        super(debitCardService);
        this.debitCardService = debitCardService;
    }

    @PutMapping("/{idDebitCard}")
    public Mono<DebitCard> update(@PathVariable String idDebitCard, @Valid @RequestBody DebitCard debitCard) {
        return debitCardService.update(idDebitCard, debitCard);
    }
    @GetMapping("/get-balance-principal-account/{idDebitCard}")
    public Mono<Map<String, Object>> getBalancePrincipalAccount(@PathVariable String idDebitCard) {
        return debitCardService.getBalanceOfBankAccountInDebitCard(idDebitCard);
    }
    @PostMapping("/add-bank-account")
    public Mono<BankAccountDebitCard> addBankAccountToDebitCard(
            @Valid @RequestBody BankAccountDebitCard BankAccountDebitCard) {
        return debitCardService.addBankAccountToDebitCard(BankAccountDebitCard);
    }

    @GetMapping("/findByIdWithBankAccountsOrderByCreatedAt/{idDebitCard}")
    public Mono<DebitCard> findByIdWithBankAccountsOrderByCreatedAt(@PathVariable String idDebitCard) {
        return debitCardService.findByIdWithBankAccountsOrderByCreatedAt(idDebitCard);
    }

}
