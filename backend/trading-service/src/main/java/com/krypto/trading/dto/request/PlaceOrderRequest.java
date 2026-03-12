package com.krypto.trading.dto.request;

import com.krypto.trading.entity.OrderSide;
import com.krypto.trading.entity.OrderType;
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
public class PlaceOrderRequest {

    @NotNull(message = "coinId is required")
    private UUID coinId;

    @NotNull(message = "type is required")
    private OrderType type;

    @NotNull(message = "side is required")
    private OrderSide side;

    @Positive(message = "price must be positive")
    private BigDecimal price;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;
}
