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
public class SettleTradeRequest {

    @NotNull(message = "buyerId is required")
    private UUID buyerId;

    @NotNull(message = "sellerId is required")
    private UUID sellerId;

    @NotNull(message = "coinId is required")
    private UUID coinId;

    @NotNull(message = "coinSymbol is required")
    private String coinSymbol;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "price is required")
    @Positive(message = "price must be positive")
    private BigDecimal price;

    @NotNull(message = "fee is required")
    private BigDecimal fee;
}
