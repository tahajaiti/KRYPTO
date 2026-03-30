package com.krypto.trading.mapper;

import com.krypto.trading.dto.request.PlaceOrderRequest;
import com.krypto.trading.dto.response.OrderResponse;
import com.krypto.trading.dto.response.TradeResponse;
import com.krypto.trading.entity.Order;
import com.krypto.trading.entity.Trade;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TradingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "filledAmount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Order toEntity(PlaceOrderRequest request);

    OrderResponse toOrderResponse(Order order);

    List<OrderResponse> toOrderResponses(List<Order> orders);

    TradeResponse toTradeResponse(Trade trade);

    List<TradeResponse> toTradeResponses(List<Trade> trades);
}
