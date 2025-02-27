package com.bank.appbank.controller;

import com.bank.appbank.model.DebitCard;
import com.bank.appbank.service.DebitCardService;
import com.bank.appbank.service.ServiceT;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/debit-cards")
public class DebitCardController extends ControllerT<DebitCard, String>{
    private DebitCardService debitCardService;
    public DebitCardController(DebitCardService debitCardService) {
        super(debitCardService);
    }

}
