package com.krypto.trading.service;

import com.krypto.common.dto.PageResponse;
import com.krypto.trading.dto.request.PlaceOrderRequest;
import com.krypto.trading.dto.response.OrderResponse;
import com.krypto.trading.dto.response.TradeResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TradingService {

    OrderResponse placeOrder(PlaceOrderRequest request);

    OrderResponse getOrderById(UUID orderId);

    PageResponse<OrderResponse> getMyOrders(Pageable pageable);

    OrderResponse cancelOrder(UUID orderId);

    PageResponse<TradeResponse> getMyTrades(Pageable pageable);

    PageResponse<TradeResponse> getTradesByCoin(UUID coinId, Pageable pageable);
}
