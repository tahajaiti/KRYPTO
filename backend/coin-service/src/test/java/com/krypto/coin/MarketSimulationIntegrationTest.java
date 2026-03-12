package com.krypto.coin;

import com.krypto.coin.entity.Coin;
import com.krypto.coin.repository.CoinRepository;
import com.krypto.coin.repository.PriceHistoryRepository;
import com.krypto.coin.service.MarketSimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
@TestPropertySource(properties = {
        "market-simulation.enabled=true",
        "market-simulation.max-coins-per-tick=1",
        "market-simulation.base-volatility-percent=1.0",
        "market-simulation.shock-min-percent=2.0",
        "market-simulation.shock-max-percent=2.0",
        "market-simulation.volume-min-factor=1.0",
        "market-simulation.volume-max-factor=1.0"
})
class MarketSimulationIntegrationTest {

    @Autowired
    private MarketSimulationService marketSimulationService;

    @Autowired
    private CoinRepository coinRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        priceHistoryRepository.deleteAll();
        coinRepository.deleteAll();

        Coin coin = Coin.builder()
                .name("Sim Coin")
                .symbol("SIM")
                .image(null)
                .initialSupply(new BigDecimal("1000000"))
                .currentSupply(new BigDecimal("1000000"))
                .creatorId(UUID.randomUUID())
                .creationFee(new BigDecimal("100"))
                .currentPrice(new BigDecimal("1.0"))
                .marketCap(new BigDecimal("1000000"))
                .active(true)
                .build();

        coinRepository.save(coin);
    }

    @Test
    void shouldApplySimulationTickAndRecordPriceHistory() {
        Coin before = coinRepository.findAll().get(0);
        BigDecimal oldPrice = before.getCurrentPrice();

        marketSimulationService.runSimulationTick();

        Coin after = coinRepository.findById(before.getId()).orElseThrow();
        assertThat(after.getCurrentPrice()).isNotNull();
        assertThat(after.getCurrentPrice()).isNotEqualByComparingTo(oldPrice);
        assertThat(after.getMarketCap()).isNotNull();

        assertThat(priceHistoryRepository.count()).isEqualTo(1);

        Mockito.verify(rabbitTemplate, Mockito.atLeastOnce())
            .convertAndSend(eq("market.exchange"), eq("market.simulated"), any(Object.class));
    }
}
