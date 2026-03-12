package com.krypto.wallet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MintCoinRequest {

    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "coinId is required")
    private UUID coinId;

    @NotNull(message = "symbol is required")
    private String symbol;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;
}
