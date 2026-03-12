package com.krypto.trading.dto.response;

import com.krypto.trading.entity.OrderSide;
import com.krypto.trading.entity.OrderStatus;
import com.krypto.trading.entity.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private UUID id;
    private UUID userId;
    private UUID coinId;
    private OrderType type;
    private OrderSide side;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal filledAmount;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
