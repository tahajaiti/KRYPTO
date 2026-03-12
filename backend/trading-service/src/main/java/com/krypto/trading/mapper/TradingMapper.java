package com.krypto.trading.mapper;

import com.krypto.trading.dto.response.OrderResponse;
import com.krypto.trading.dto.response.TradeResponse;
import com.krypto.trading.entity.Order;
import com.krypto.trading.entity.Trade;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TradingMapper {

    OrderResponse toOrderResponse(Order order);

    List<OrderResponse> toOrderResponses(List<Order> orders);

    TradeResponse toTradeResponse(Trade trade);

    List<TradeResponse> toTradeResponses(List<Trade> trades);
}
