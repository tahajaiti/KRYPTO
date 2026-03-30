package com.krypto.trading;

import com.krypto.common.dto.ApiResponse;
import com.krypto.common.security.JwtPrincipal;
import com.krypto.trading.client.CoinClient;
import com.krypto.trading.client.WalletClient;
import com.krypto.trading.client.dto.BalanceItemResponse;
import com.krypto.trading.client.dto.CoinPriceResponse;
import com.krypto.trading.client.dto.RecordTradeRequest;
import com.krypto.trading.client.dto.SettleTradeRequest;
import com.krypto.trading.dto.request.PlaceOrderRequest;
import com.krypto.trading.entity.OrderSide;
import com.krypto.trading.entity.OrderType;
import com.krypto.trading.repository.OrderRepository;
import com.krypto.trading.repository.TradeRepository;
import com.krypto.trading.service.TradingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
class TradingServiceFailureIntegrationTest {

    @Autowired
    private TradingService tradingService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @MockitoBean
    private WalletClient walletClient;

    @MockitoBean
    private CoinClient coinClient;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private UUID buyerId;
    private UUID sellerId;
    private UUID coinId;

    @BeforeEach
    void setUp() {
        tradeRepository.deleteAll();
        orderRepository.deleteAll();

        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        coinId = UUID.randomUUID();

        CoinPriceResponse priceResponse = CoinPriceResponse.builder()
                .coinId(coinId)
                .symbol("MBTC")
                .currentPrice(new BigDecimal("10"))
                .build();

        BalanceItemResponse krypBalance = BalanceItemResponse.builder()
                .symbol("KRYP")
                .balance(new BigDecimal("100000"))
                .build();

        BalanceItemResponse coinBalance = BalanceItemResponse.builder()
                .coinId(coinId)
                .symbol("MBTC")
                .balance(new BigDecimal("100000"))
                .build();

        Mockito.when(coinClient.getCoinPrice(eq(coinId))).thenReturn(ApiResponse.ok(priceResponse));
        Mockito.when(coinClient.recordTrade(eq(coinId), any(RecordTradeRequest.class), eq("krypto-internal-secret")))
                .thenReturn(ApiResponse.ok(priceResponse));
        Mockito.when(walletClient.getKrypBalance(any(UUID.class), eq("krypto-internal-secret")))
                .thenReturn(ApiResponse.ok(krypBalance));
        Mockito.when(walletClient.getCoinBalance(any(UUID.class), any(UUID.class), eq("krypto-internal-secret")))
                .thenReturn(ApiResponse.ok(coinBalance));
        Mockito.when(walletClient.settleTrade(any(SettleTradeRequest.class), eq("krypto-internal-secret")))
                .thenReturn(ApiResponse.ok(null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldFailWhenBuyerHasInsufficientKryp() {
        Mockito.when(walletClient.settleTrade(any(SettleTradeRequest.class), eq("krypto-internal-secret")))
                .thenThrow(new RuntimeException("buyer has insufficient KRYP"));

        placeLimitSellOrder();

        authenticateAs(buyerId, "buyer", "PLAYER");
        assertThatCode(() -> tradingService.placeOrder(new PlaceOrderRequest(
                coinId,
                OrderType.LIMIT,
                OrderSide.BUY,
                new BigDecimal("10"),
                new BigDecimal("5"))))
                .doesNotThrowAnyException();

        assertThat(tradeRepository.count()).isEqualTo(1);
        Mockito.verify(rabbitTemplate, Mockito.times(1))
                                .convertAndSend(eq("trading.exchange"), eq("trade.executed"), any(Object.class));
    }

    @Test
    void shouldFailWhenSellerHasInsufficientCoinBalance() {
        Mockito.when(walletClient.settleTrade(any(SettleTradeRequest.class), eq("krypto-internal-secret")))
                .thenThrow(new RuntimeException("seller has insufficient coin balance"));

        placeLimitBuyOrder();

        authenticateAs(sellerId, "seller", "PLAYER");
        assertThatCode(() -> tradingService.placeOrder(new PlaceOrderRequest(
                coinId,
                OrderType.LIMIT,
                OrderSide.SELL,
                new BigDecimal("10"),
                new BigDecimal("5"))))
                .doesNotThrowAnyException();

        assertThat(tradeRepository.count()).isEqualTo(1);
        Mockito.verify(rabbitTemplate, Mockito.times(1))
                                .convertAndSend(eq("trading.exchange"), eq("trade.executed"), any(Object.class));
    }

    @Test
    void shouldFailWhenCoinServiceRejectsInternalSecret() {
        Mockito.when(coinClient.recordTrade(eq(coinId), any(RecordTradeRequest.class), eq("krypto-internal-secret")))
                .thenThrow(new RuntimeException("403 invalid internal secret"));

        placeLimitSellOrder();

        authenticateAs(buyerId, "buyer", "PLAYER");
        assertThatCode(() -> tradingService.placeOrder(new PlaceOrderRequest(
                coinId,
                OrderType.LIMIT,
                OrderSide.BUY,
                new BigDecimal("10"),
                new BigDecimal("5"))))
                .doesNotThrowAnyException();

        assertThat(tradeRepository.count()).isEqualTo(1);
        Mockito.verify(rabbitTemplate, Mockito.times(1))
                                .convertAndSend(eq("trading.exchange"), eq("trade.executed"), any(Object.class));
    }

    private void placeLimitSellOrder() {
        authenticateAs(sellerId, "seller", "PLAYER");
        tradingService.placeOrder(new PlaceOrderRequest(
                coinId,
                OrderType.LIMIT,
                OrderSide.SELL,
                new BigDecimal("10"),
                new BigDecimal("5")));
    }

    private void placeLimitBuyOrder() {
        authenticateAs(buyerId, "buyer", "PLAYER");
        tradingService.placeOrder(new PlaceOrderRequest(
                coinId,
                OrderType.LIMIT,
                OrderSide.BUY,
                new BigDecimal("10"),
                new BigDecimal("5")));
    }

    private void authenticateAs(UUID userId, String username, String role) {
        JwtPrincipal principal = new JwtPrincipal(userId, username, role);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
