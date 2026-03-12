package com.krypto.trading.controller;

import com.krypto.common.dto.ApiResponse;
import com.krypto.common.dto.PageResponse;
import com.krypto.trading.dto.request.PlaceOrderRequest;
import com.krypto.trading.dto.response.LeaderboardEntryResponse;
import com.krypto.trading.dto.response.OrderResponse;
import com.krypto.trading.dto.response.TradeResponse;
import com.krypto.trading.service.TradingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradingController {

    private final TradingService tradingService;

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse response = tradingService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "order placed"));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID orderId) {
        OrderResponse response = tradingService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/orders/me")
    public ResponseEntity<PageResponse<OrderResponse>> getMyOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(tradingService.getMyOrders(pageable));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable UUID orderId) {
        OrderResponse response = tradingService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response, "order cancelled"));
    }

    @GetMapping("/me")
    public ResponseEntity<PageResponse<TradeResponse>> getMyTrades(
            @PageableDefault(size = 20, sort = "executedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(tradingService.getMyTrades(pageable));
    }

    @GetMapping("/coin/{coinId}")
    public ResponseEntity<PageResponse<TradeResponse>> getTradesByCoin(
            @PathVariable UUID coinId,
            @PageableDefault(size = 20, sort = "executedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(tradingService.getTradesByCoin(coinId, pageable));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tradingService.getLeaderboard(limit)));
    }
}
