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
import com.krypto.trading.dto.response.LeaderboardEntryResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradingServiceImpl implements com.krypto.trading.service.TradingService {

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

        validatePlaceOrderRequest(request);
        CoinPriceResponse coinPrice = getCoinPrice(request.getCoinId());

        Order order = Order.builder()
                .userId(userId)
                .coinId(request.getCoinId())
                .type(request.getType())
                .side(request.getSide())
                .price(request.getType() == OrderType.MARKET ? null : request.getPrice())
                .amount(request.getAmount())
                .filledAmount(BigDecimal.ZERO)
                .status(OrderStatus.OPEN)
                .build();

        order = orderRepository.save(order);
        matchOrder(order, coinPrice);

        if (order.getType() == OrderType.MARKET && remaining(order).compareTo(BigDecimal.ZERO) > 0) {
            order.setStatus(OrderStatus.CANCELLED);
        }

        return tradingMapper.toOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        AuthorizationUtils.requireSelfOrRole(order.getUserId(), "ADMIN");
        return tradingMapper.toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(Pageable pageable) {
        UUID userId = AuthorizationUtils.requireUserId();
        Page<Order> page = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        Sort.Order sortOrder = pageable.getSort().stream().findFirst().orElse(null);
        String sortBy = sortOrder != null ? sortOrder.getProperty() : null;
        String sortDirection = sortOrder != null ? sortOrder.getDirection().name() : null;

        return PageResponse.of(
                tradingMapper.toOrderResponses(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getNumberOfElements(),
                sortBy,
                sortDirection
        );
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        UUID userId = AuthorizationUtils.requireUserId();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getUserId().equals(userId) && !AuthorizationUtils.hasRole("ADMIN")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "cannot cancel another user's order");
        }

        if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "order cannot be cancelled in current status");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return tradingMapper.toOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TradeResponse> getMyTrades(Pageable pageable) {
        UUID userId = AuthorizationUtils.requireUserId();
        Page<Trade> page = tradeRepository.findByBuyerIdOrSellerIdOrderByExecutedAtDesc(userId, userId, pageable);

        Sort.Order sortOrder = pageable.getSort().stream().findFirst().orElse(null);
        String sortBy = sortOrder != null ? sortOrder.getProperty() : null;
        String sortDirection = sortOrder != null ? sortOrder.getDirection().name() : null;

        return PageResponse.of(
                tradingMapper.toTradeResponses(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getNumberOfElements(),
                sortBy,
                sortDirection
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TradeResponse> getTradesByCoin(UUID coinId, Pageable pageable) {
        AuthorizationUtils.requireRole("ADMIN");

        Page<Trade> page = tradeRepository.findByCoinIdOrderByExecutedAtDesc(coinId, pageable);

        Sort.Order sortOrder = pageable.getSort().stream().findFirst().orElse(null);
        String sortBy = sortOrder != null ? sortOrder.getProperty() : null;
        String sortDirection = sortOrder != null ? sortOrder.getDirection().name() : null;

        return PageResponse.of(
                tradingMapper.toTradeResponses(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getNumberOfElements(),
                sortBy,
                sortDirection
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "trading-leaderboard", key = "#limit")
    public List<LeaderboardEntryResponse> getLeaderboard(int limit) {
        AuthorizationUtils.requireRole("ADMIN");

        int resolvedLimit = Math.max(1, Math.min(100, limit));
        List<Trade> trades = tradeRepository.findAllByOrderByExecutedAtDesc();

        Map<UUID, LeaderboardAccumulator> stats = new HashMap<>();
        for (Trade trade : trades) {
            BigDecimal notional = trade.getPrice().multiply(trade.getAmount());
            accumulate(stats, trade.getBuyerId(), trade.getAmount(), notional);
            accumulate(stats, trade.getSellerId(), trade.getAmount(), notional);
        }

        List<LeaderboardEntryResponse> entries = new ArrayList<>();
        for (Map.Entry<UUID, LeaderboardAccumulator> entry : stats.entrySet()) {
            LeaderboardAccumulator value = entry.getValue();
            entries.add(LeaderboardEntryResponse.builder()
                    .userId(entry.getKey())
                    .totalVolume(value.totalVolume)
                    .totalNotional(value.totalNotional)
                    .trades(value.trades)
                    .build());
        }

        return entries.stream()
                .sorted(Comparator
                        .comparing(LeaderboardEntryResponse::getTotalNotional).reversed()
                        .thenComparing(LeaderboardEntryResponse::getTotalVolume).reversed())
                .limit(resolvedLimit)
                .toList();
    }

    private void accumulate(Map<UUID, LeaderboardAccumulator> stats, UUID userId, BigDecimal volume, BigDecimal notional) {
        LeaderboardAccumulator item = stats.computeIfAbsent(userId, key -> new LeaderboardAccumulator());
        item.totalVolume = item.totalVolume.add(volume);
        item.totalNotional = item.totalNotional.add(notional);
        item.trades++;
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
            if (makerRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal tradeAmount = remaining(takerOrder).min(makerRemaining);
            if (tradeAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal executionPrice = resolveExecutionPrice(takerOrder, makerOrder, coinPrice.getCurrentPrice());
            BigDecimal notional = executionPrice.multiply(tradeAmount);
            BigDecimal fee = notional.multiply(feeRate).setScale(18, RoundingMode.HALF_UP);

            UUID buyerId = takerOrder.getSide() == OrderSide.BUY ? takerOrder.getUserId() : makerOrder.getUserId();
            UUID sellerId = takerOrder.getSide() == OrderSide.SELL ? takerOrder.getUserId() : makerOrder.getUserId();

            settleTradeWithWallet(buyerId, sellerId, takerOrder.getCoinId(), coinPrice.getSymbol(), tradeAmount, executionPrice, fee);
            updateCoinPrice(takerOrder.getCoinId(), executionPrice, tradeAmount);

            Trade trade = Trade.builder()
                    .buyOrderId(takerOrder.getSide() == OrderSide.BUY ? takerOrder.getId() : makerOrder.getId())
                    .sellOrderId(takerOrder.getSide() == OrderSide.SELL ? takerOrder.getId() : makerOrder.getId())
                    .coinId(takerOrder.getCoinId())
                    .buyerId(buyerId)
                    .sellerId(sellerId)
                    .price(executionPrice)
                    .amount(tradeAmount)
                    .fee(fee)
                    .executedAt(Instant.now())
                    .build();
            tradeRepository.save(trade);

            publishToBlockchain(trade.getId(), trade.getExecutedAt(), sellerId, buyerId, coinPrice.getSymbol(), tradeAmount, fee);
            publishToGamification(trade.getId(), buyerId, sellerId, takerOrder.getCoinId(), tradeAmount, executionPrice, notional);

            takerOrder.setFilledAmount(takerOrder.getFilledAmount().add(tradeAmount));
            makerOrder.setFilledAmount(makerOrder.getFilledAmount().add(tradeAmount));

            updateStatus(takerOrder);
            updateStatus(makerOrder);
            orderRepository.save(makerOrder);
        }

        updateStatus(takerOrder);
    }

    private List<Order> findMatchCandidates(Order order) {
        OrderSide makerSide = order.getSide() == OrderSide.BUY ? OrderSide.SELL : OrderSide.BUY;

        if (order.getType() == OrderType.MARKET) {
            if (order.getSide() == OrderSide.BUY) {
                return orderRepository.findByCoinIdAndSideAndStatusInOrderByPriceAscCreatedAtAsc(order.getCoinId(), makerSide, MATCHABLE_STATUSES);
            }
            return orderRepository.findByCoinIdAndSideAndStatusInOrderByPriceDescCreatedAtAsc(order.getCoinId(), makerSide, MATCHABLE_STATUSES);
        }

        if (order.getSide() == OrderSide.BUY) {
            return orderRepository.findByCoinIdAndSideAndStatusInAndPriceLessThanEqualOrderByPriceAscCreatedAtAsc(
                    order.getCoinId(), makerSide, MATCHABLE_STATUSES, order.getPrice());
        }

        return orderRepository.findByCoinIdAndSideAndStatusInAndPriceGreaterThanEqualOrderByPriceDescCreatedAtAsc(
                order.getCoinId(), makerSide, MATCHABLE_STATUSES, order.getPrice());
    }

    private BigDecimal resolveExecutionPrice(Order takerOrder, Order makerOrder, BigDecimal fallbackPrice) {
        if (makerOrder.getPrice() != null) {
            return makerOrder.getPrice();
        }
        if (takerOrder.getPrice() != null) {
            return takerOrder.getPrice();
        }
        return fallbackPrice;
    }

    private BigDecimal remaining(Order order) {
        return order.getAmount().subtract(order.getFilledAmount());
    }

    private void updateStatus(Order order) {
        BigDecimal remaining = remaining(order);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            order.setStatus(OrderStatus.FILLED);
            return;
        }

        if (order.getFilledAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (order.getStatus() != OrderStatus.CANCELLED) {
                order.setStatus(OrderStatus.PARTIALLY_FILLED);
            }
            return;
        }

        if (order.getStatus() != OrderStatus.CANCELLED) {
            order.setStatus(OrderStatus.OPEN);
        }
    }

    private void validatePlaceOrderRequest(PlaceOrderRequest request) {
        if (request.getType() == OrderType.LIMIT && request.getPrice() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "price is required for limit orders");
        }

        if (request.getType() == OrderType.MARKET && request.getPrice() != null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "price must be omitted for market orders");
        }
    }

    private CoinPriceResponse getCoinPrice(UUID coinId) {
        try {
            var response = coinClient.getCoinPrice(coinId);
            if (response == null || response.getData() == null || response.getData().getCurrentPrice() == null) {
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "coin-service price unavailable");
            }
            return response.getData();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "failed to fetch coin price", ex);
        }
    }

    private void settleTradeWithWallet(UUID buyerId,
                                       UUID sellerId,
                                       UUID coinId,
                                       String coinSymbol,
                                       BigDecimal amount,
                                       BigDecimal price,
                                       BigDecimal fee) {
        try {
            walletClient.settleTrade(
                    SettleTradeRequest.builder()
                            .buyerId(buyerId)
                            .sellerId(sellerId)
                            .coinId(coinId)
                            .coinSymbol(coinSymbol)
                            .amount(amount)
                            .price(price)
                            .fee(fee)
                            .build(),
                    internalSecret
            );
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "wallet settlement failed", ex);
        }
    }

    private void updateCoinPrice(UUID coinId, BigDecimal executionPrice, BigDecimal tradeAmount) {
        try {
            coinClient.recordTrade(
                    coinId,
                    new RecordTradeRequest(executionPrice, tradeAmount),
                    internalSecret
            );
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "coin price update failed", ex);
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

        private void publishToGamification(UUID tradeId,
                           UUID buyerId,
                           UUID sellerId,
                           UUID coinId,
                           BigDecimal amount,
                           BigDecimal price,
                           BigDecimal notional) {
        long quantity = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        long priceInKryp = price.setScale(0, RoundingMode.HALF_UP).longValue();
        long notionalValue = notional.setScale(0, RoundingMode.HALF_UP).longValue();
        long executedAt = Instant.now().toEpochMilli();

        Map<String, Object> buyerMessage = Map.of(
            "tradeId", tradeId.toString(),
            "userId", buyerId.toString(),
            "coinId", coinId.toString(),
            "quantity", quantity,
            "priceInKryp", priceInKryp,
            "notionalValue", notionalValue,
            "side", "BUY",
            "executedAt", executedAt
        );

        Map<String, Object> sellerMessage = Map.of(
            "tradeId", tradeId.toString(),
            "userId", sellerId.toString(),
            "coinId", coinId.toString(),
            "quantity", quantity,
            "priceInKryp", priceInKryp,
            "notionalValue", notionalValue,
            "side", "SELL",
            "executedAt", executedAt
        );

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.GAMIFICATION_EXCHANGE,
            RabbitMQConfig.TRADE_EXECUTED_ROUTING_KEY,
            buyerMessage
        );

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.GAMIFICATION_EXCHANGE,
            RabbitMQConfig.TRADE_EXECUTED_ROUTING_KEY,
            sellerMessage
        );
        }

    private static class LeaderboardAccumulator {
        private BigDecimal totalVolume = BigDecimal.ZERO;
        private BigDecimal totalNotional = BigDecimal.ZERO;
        private long trades = 0;
    }
}
