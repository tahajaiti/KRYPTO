package com.krypto.trading.service.impl;

import com.krypto.common.dto.PageResponse;
import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;
import com.krypto.common.exception.ResourceNotFoundException;
import com.krypto.common.security.AuthorizationUtils;
import com.krypto.trading.client.CoinClient;
import com.krypto.trading.client.WalletClient;
import com.krypto.trading.client.dto.CoinPriceResponse;
import com.krypto.trading.client.dto.RecordTradeRequest;
import com.krypto.trading.client.dto.SettleTradeRequest;
import com.krypto.trading.config.RabbitMQConfig;
import com.krypto.trading.dto.request.PlaceOrderRequest;
import com.krypto.trading.dto.response.OrderResponse;
import com.krypto.trading.dto.response.TradeResponse;
import com.krypto.trading.entity.Order;
import com.krypto.trading.entity.OrderSide;
import com.krypto.trading.entity.OrderStatus;
import com.krypto.trading.entity.OrderType;
import com.krypto.trading.entity.Trade;
import com.krypto.trading.mapper.TradingMapper;
import com.krypto.trading.repository.OrderRepository;
import com.krypto.trading.repository.TradeRepository;
import com.krypto.trading.service.TradingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingServiceImpl implements TradingService {

    private static final Set<OrderStatus> MATCHABLE_STATUSES = Set.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED);

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final TradingMapper tradingMapper;
    private final WalletClient walletClient;
    private final CoinClient coinClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${trading.fee-rate:0.01}")
    private BigDecimal feeRate;

    @Value("${krypto.internal-secret:krypto-internal-secret}")
    private String internalSecret;

    @Override
    @Transactional
    @CacheEvict(value = "trading-leaderboard", allEntries = true)
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        UUID userId = AuthorizationUtils.requireUserId();
        validateBalance(userId, request.getCoinId(), request.getSide(), request.getAmount(), request.getPrice(), request.getType());

        Order order = tradingMapper.toEntity(request);
        order.setUserId(userId);
        order.setStatus(OrderStatus.OPEN);
        order.setFilledAmount(BigDecimal.ZERO);
        
        Order savedOrder = orderRepository.save(order);

        try {
            CoinPriceResponse coinPrice = coinClient.getCoinPrice(request.getCoinId()).getData();
            matchOrder(savedOrder, coinPrice);
        } catch (Exception e) {
            log.warn("Matching failed or coin service unavailable for order {}: {}", savedOrder.getId(), e.getMessage());
        }

        return tradingMapper.toOrderResponse(orderRepository.findById(savedOrder.getId()).orElse(savedOrder));
    }

    private void validateBalance(UUID userId, UUID coinId, OrderSide side, BigDecimal amount, BigDecimal price, OrderType type) {
        try {
            if (side == OrderSide.BUY) {
                BigDecimal cost = type == OrderType.LIMIT ? price.multiply(amount) : BigDecimal.ZERO; 
                BigDecimal balance = walletClient.getKrypBalance(userId, internalSecret).getData().getBalance();
                if (type == OrderType.LIMIT && balance.compareTo(cost) < 0) {
                    throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "insufficient KRYP balance for this order");
                }
            } else {
                BigDecimal balance = walletClient.getCoinBalance(userId, coinId, internalSecret).getData().getBalance();
                if (balance.compareTo(amount) < 0) {
                    throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "insufficient coin balance for this sale");
                }
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Failed to verify balances for user {}: {}", userId, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "could not verify account balances");
        }
    }

    @Override
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        return tradingMapper.toOrderResponse(order);
    }

    @Override
    public PageResponse<OrderResponse> getMyOrders(Pageable pageable) {
        UUID userId = AuthorizationUtils.requireUserId();
        Page<Order> page = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(
                tradingMapper.toOrderResponses(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getNumberOfElements(),
                "createdAt",
                "DESC"
        );
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        
        AuthorizationUtils.requireSelfOrRole(order.getUserId(), "USER");

        if (order.getStatus() != OrderStatus.OPEN && order.getStatus() != OrderStatus.PARTIALLY_FILLED) {
            throw new BusinessException(ErrorCode.INVALID_TRANSACTION, "order cannot be cancelled in current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        return tradingMapper.toOrderResponse(order);
    }

    @Override
    public PageResponse<TradeResponse> getMyTrades(Pageable pageable) {
        UUID userId = AuthorizationUtils.requireUserId();
        Page<Trade> page = tradeRepository.findByBuyerIdOrSellerIdOrderByExecutedAtDesc(userId, userId, pageable);
        return PageResponse.of(
                tradingMapper.toTradeResponses(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getNumberOfElements(),
                "executedAt",
                "DESC"
        );
    }

    @Override
    public PageResponse<TradeResponse> getTradesByCoin(UUID coinId, Pageable pageable) {
        Page<Trade> page = tradeRepository.findByCoinIdOrderByExecutedAtDesc(coinId, pageable);
        return PageResponse.of(
                tradingMapper.toTradeResponses(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getNumberOfElements(),
                "executedAt",
                "DESC"
        );
    }

    private void matchOrder(Order takerOrder, CoinPriceResponse coinPrice) {
        List<Order> candidates = findMatchCandidates(takerOrder);
        if (candidates.isEmpty()) {
            return;
        }

        for (Order makerOrder : candidates) {
            if (remaining(takerOrder).compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal makerRemaining = remaining(makerOrder);
            BigDecimal takerRemaining = remaining(takerOrder);
            BigDecimal executionAmount = makerRemaining.min(takerRemaining);
            BigDecimal executionPrice = takerOrder.getType() == OrderType.LIMIT ? takerOrder.getPrice() : makerOrder.getPrice();

            if (takerOrder.getType() == OrderType.LIMIT) {
                if (takerOrder.getSide() == OrderSide.BUY && takerOrder.getPrice().compareTo(makerOrder.getPrice()) < 0) continue;
                if (takerOrder.getSide() == OrderSide.SELL && takerOrder.getPrice().compareTo(makerOrder.getPrice()) > 0) continue;
            }

            executeTrade(takerOrder, makerOrder, executionAmount, executionPrice);
        }
    }

    private List<Order> findMatchCandidates(Order takerOrder) {
        OrderSide counterSide = takerOrder.getSide() == OrderSide.BUY ? OrderSide.SELL : OrderSide.BUY;
        
        if (takerOrder.getSide() == OrderSide.BUY) {
            return orderRepository.findByCoinIdAndSideAndStatusInOrderByPriceAscCreatedAtAsc(
                takerOrder.getCoinId(), 
                counterSide, 
                MATCHABLE_STATUSES
            ).stream()
             .filter(o -> !o.getUserId().equals(takerOrder.getUserId()))
             .toList();
        } else {
            return orderRepository.findByCoinIdAndSideAndStatusInOrderByPriceDescCreatedAtAsc(
                takerOrder.getCoinId(), 
                counterSide, 
                MATCHABLE_STATUSES
            ).stream()
             .filter(o -> !o.getUserId().equals(takerOrder.getUserId()))
             .toList();
        }
    }

    private void executeTrade(Order taker, Order maker, BigDecimal amount, BigDecimal price) {
        BigDecimal fee = amount.multiply(price).multiply(feeRate);
        
        UUID buyerId = taker.getSide() == OrderSide.BUY ? taker.getUserId() : maker.getUserId();
        UUID sellerId = taker.getSide() == OrderSide.SELL ? taker.getUserId() : maker.getUserId();
        
        Trade trade = Trade.builder()
                .buyOrderId(taker.getSide() == OrderSide.BUY ? taker.getId() : maker.getId())
                .sellOrderId(taker.getSide() == OrderSide.SELL ? taker.getId() : maker.getId())
                .buyerId(buyerId)
                .sellerId(sellerId)
                .coinId(taker.getCoinId())
                .price(price)
                .amount(amount)
                .fee(fee)
                .executedAt(Instant.now())
                .build();

        String coinSymbol = getCoinSymbol(taker.getCoinId());
        settleOnWallet(trade, coinSymbol);
        tradeRepository.save(trade);
        updateOrderAfterFill(taker, amount);
        updateOrderAfterFill(maker, amount);
        updateMarketPrice(taker.getCoinId(), price, amount);
        publishToBlockchain(trade.getId(), trade.getExecutedAt(), buyerId, sellerId, coinSymbol, amount, fee);
    }

    private void updateOrderAfterFill(Order order, BigDecimal fillAmount) {
        order.setFilledAmount(order.getFilledAmount().add(fillAmount));
        if (remaining(order).compareTo(BigDecimal.ZERO) <= 0) {
            order.setStatus(OrderStatus.FILLED);
        } else {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        }
        orderRepository.save(order);
    }

    private BigDecimal remaining(Order order) {
        return order.getAmount().subtract(order.getFilledAmount());
    }

    private void settleOnWallet(Trade trade, String coinSymbol) {
        walletClient.settleTrade(
            new SettleTradeRequest(
                trade.getBuyerId(),
                trade.getSellerId(),
                trade.getCoinId(),
                coinSymbol,
                trade.getAmount(),
                trade.getPrice(),
                trade.getFee()
            ),
            internalSecret
        );
    }

    private String getCoinSymbol(UUID coinId) {
        try {
            CoinPriceResponse coinPrice = coinClient.getCoinPrice(coinId).getData();
            return coinPrice.getSymbol();
        } catch (Exception e) {
            log.error("Failed to fetch coin symbol for coin {}: {}", coinId, e.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "failed to fetch coin details");
        }
    }

    private void updateMarketPrice(UUID coinId, BigDecimal executionPrice, BigDecimal tradeAmount) {
        try {
            coinClient.recordTrade(
                    coinId,
                    new RecordTradeRequest(executionPrice, tradeAmount),
                    internalSecret
            );
        } catch (Exception ex) {
            log.warn("Coin price update failed for coin {}: {}", coinId, ex.getMessage());
        }
    }

    private void publishToBlockchain(UUID tradeId,
                                     Instant executedAt,
                                     UUID fromUserId,
                                     UUID toUserId,
                                     String coinSymbol,
                                     BigDecimal amount,
                                     BigDecimal fee) {
        Map<String, Object> message = Map.of(
                "type", "TRADE",
                "sourceEventId", tradeId.toString(),
                "eventTimestamp", executedAt.toEpochMilli(),
                "fromUserId", fromUserId.toString(),
                "toUserId", toUserId.toString(),
                "coinSymbol", coinSymbol,
                "amount", amount,
                "fee", fee
        );

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.TRADING_EXCHANGE,
            RabbitMQConfig.TRADE_EXECUTED_ROUTING_KEY,
                message
        );
    }
}
