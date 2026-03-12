package com.krypto.blockchain.dto.request;

import com.krypto.blockchain.model.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddTransactionRequest {

    @NotNull(message = "type is required")
    private TransactionType type;

    private String fromUserId;

    private String toUserId;

    private String coinSymbol;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    private BigDecimal fee;
}
