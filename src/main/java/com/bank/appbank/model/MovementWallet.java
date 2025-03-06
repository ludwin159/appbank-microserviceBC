package com.bank.appbank.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "movements_wallet")
public class MovementWallet {
    @NotNull
    private String id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS",timezone = "GMT")
    private Date createdAt;

    @NotNull
    private String idWallet;
    @NotNull
    private TypeMovementWallet type;
    @NotNull
    private String description;
    @NotNull
    private Double amount;
    @NotNull
    private String idDebitCard;

    @NotNull
    private String numberDestin;
    @NotNull
    private StateMovement stateMovement;

    public enum TypeMovementWallet {MAKE_PAYMENT, RECEIPT_PAYMENT}
    public enum StateMovement {APPROVE, PENDING, REJECTED}
}
