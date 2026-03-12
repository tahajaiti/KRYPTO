package com.krypto.trading;

import com.krypto.common.dto.ApiResponse;
import com.krypto.common.dto.PageResponse;
import com.krypto.common.security.JwtPrincipal;
import com.krypto.trading.client.CoinClient;
import com.krypto.trading.client.WalletClient;
import com.krypto.trading.client.dto.CoinPriceResponse;
import com.krypto.trading.client.dto.RecordTradeRequest;
import com.krypto.trading.client.dto.SettleTradeRequest;
import com.krypto.trading.dto.request.PlaceOrderRequest;
import com.krypto.trading.dto.response.OrderResponse;
import com.krypto.trading.dto.response.TradeResponse;
import com.krypto.trading.entity.OrderSide;
import com.krypto.trading.entity.OrderType;
import com.krypto.trading.repository.OrderRepository;
import com.krypto.trading.repository.TradeRepository;
import com.krypto.trading.service.TradingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
class TradingServiceIntegrationTest {

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

        Mockito.when(coinClient.getCoinPrice(eq(coinId))).thenReturn(ApiResponse.ok(priceResponse));
        Mockito.when(coinClient.recordTrade(eq(coinId), any(RecordTradeRequest.class), eq("krypto-internal-secret")))
                .thenReturn(ApiResponse.ok(priceResponse));
        Mockito.when(walletClient.settleTrade(any(SettleTradeRequest.class), eq("krypto-internal-secret")))
                .thenReturn(ApiResponse.ok(null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldExecuteLimitMatchAndPublishBlockchainTransaction() {
        authenticateAs(sellerId, "seller", "PLAYER");
        OrderResponse sellOrder = tradingService.placeOrder(new PlaceOrderRequest(
                coinId,
                OrderType.LIMIT,
                OrderSide.SELL,
                new BigDecimal("10"),
                new BigDecimal("5")));

        authenticateAs(buyerId, "buyer", "PLAYER");
        OrderResponse buyOrder = tradingService.placeOrder(new PlaceOrderRequest(
                coinId,
                OrderType.LIMIT,
                OrderSide.BUY,
                new BigDecimal("10"),
                new BigDecimal("5")));

        assertThat(sellOrder.getId()).isNotNull();
        assertThat(buyOrder.getId()).isNotNull();

        var allTrades = tradeRepository.findAll();
        assertThat(allTrades).hasSize(1);
        assertThat(allTrades.get(0).getAmount()).isEqualByComparingTo("5");
        assertThat(allTrades.get(0).getPrice()).isEqualByComparingTo("10");

        Mockito.verify(walletClient, Mockito.times(1))
                .settleTrade(any(SettleTradeRequest.class), eq("krypto-internal-secret"));
        Mockito.verify(coinClient, Mockito.times(1))
                .recordTrade(eq(coinId), any(RecordTradeRequest.class), eq("krypto-internal-secret"));

        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(rabbitTemplate, Mockito.times(1))
                .convertAndSend(eq("trading.exchange"), eq("trade.executed"), messageCaptor.capture());

        Object payload = messageCaptor.getValue();
        assertThat(payload).isInstanceOf(Map.class);
        Map<?, ?> event = (Map<?, ?>) payload;
        assertThat(event.get("type")).isEqualTo("TRADE");
        assertThat(event.get("coinSymbol")).isEqualTo("MBTC");
    }

    @Test
    void shouldCreatePartialFillAndLeaveRemainderOpen() {
        authenticateAs(sellerId, "seller", "PLAYER");
        tradingService.placeOrder(new PlaceOrderRequest(
                coinId,
                OrderType.LIMIT,
                OrderSide.SELL,
                new BigDecimal("10"),
                new BigDecimal("2")));

        authenticateAs(buyerId, "buyer", "PLAYER");
        OrderResponse buyOrder = tradingService.placeOrder(new PlaceOrderRequest(
                coinId,
                OrderType.LIMIT,
                OrderSide.BUY,
                new BigDecimal("10"),
                new BigDecimal("5")));

        OrderResponse reloaded = tradingService.getOrderById(buyOrder.getId());
        assertThat(reloaded.getFilledAmount()).isEqualByComparingTo("2");
        assertThat(reloaded.getStatus().name()).isEqualTo("PARTIALLY_FILLED");

        PageResponse<TradeResponse> myTrades = tradingService.getMyTrades(PageRequest.of(0, 10));
        assertThat(myTrades.getContent()).hasSize(1);
        assertThat(myTrades.getContent().get(0).getAmount()).isEqualByComparingTo("2");
    }

    private void authenticateAs(UUID userId, String username, String role) {
        JwtPrincipal principal = new JwtPrincipal(userId, username, role);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
