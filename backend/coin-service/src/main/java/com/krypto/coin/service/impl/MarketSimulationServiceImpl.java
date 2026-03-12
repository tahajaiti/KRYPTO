package com.krypto.coin.service.impl;

import com.krypto.coin.entity.Coin;
import com.krypto.coin.entity.PriceHistory;
import com.krypto.coin.config.RabbitMQConfig;
import com.krypto.coin.repository.CoinRepository;
import com.krypto.coin.repository.PriceHistoryRepository;
import com.krypto.coin.service.MarketSimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketSimulationServiceImpl implements MarketSimulationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MIN_VOLUME = new BigDecimal("0.000001");

    private final CoinRepository coinRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${market-simulation.enabled:true}")
    private boolean enabled;

    @Value("${market-simulation.tick-ms:30000}")
    private long tickMs;

    @Value("${market-simulation.max-coins-per-tick:10}")
    private int maxCoinsPerTick;

    @Value("${market-simulation.base-volatility-percent:1.2}")
    private BigDecimal baseVolatilityPercent;

    @Value("${market-simulation.max-change-percent:20}")
    private BigDecimal maxChangePercent;

    @Value("${market-simulation.min-price:0.0001}")
    private BigDecimal minPrice;

    @Value("${market-simulation.shock-probability:0.12}")
    private BigDecimal shockProbability;

    @Value("${market-simulation.shock-min-percent:4}")
    private BigDecimal shockMinPercent;

    @Value("${market-simulation.shock-max-percent:12}")
    private BigDecimal shockMaxPercent;

    @Value("${market-simulation.trend-bias-percent:0}")
    private BigDecimal trendBiasPercent;

    @Value("${market-simulation.volume-min-factor:0.3}")
    private BigDecimal volumeMinFactor;

    @Value("${market-simulation.volume-max-factor:2.0}")
    private BigDecimal volumeMaxFactor;

    @Scheduled(fixedDelayString = "${market-simulation.tick-ms:30000}", initialDelayString = "${market-simulation.initial-delay-ms:15000}")
    @Transactional
    public void runScheduledTick() {
        runSimulationTick();
    }

    @Transactional
    @CacheEvict(value = "coin-price", allEntries = true)
    public void runSimulationTick() {
        if (!enabled) {
            return;
        }

        List<Coin> activeCoins = coinRepository.findByActiveTrue();
        if (activeCoins.isEmpty()) {
            return;
        }

        List<Coin> selected = pickCoins(activeCoins);
        if (selected.isEmpty()) {
            return;
        }

        MarketRegime regime = drawRegime();
        BigDecimal regimeShock = drawRegimeShock(regime);

        List<PriceHistory> historyRows = new ArrayList<>(selected.size());
        int updated = 0;

        for (Coin coin : selected) {
            BigDecimal oldPrice = coin.getCurrentPrice();
            if (oldPrice == null || oldPrice.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal idiosyncratic = randomBetween(baseVolatilityPercent.negate(), baseVolatilityPercent);
            BigDecimal totalChangePercent = clamp(regimeShock.add(idiosyncratic).add(trendBiasPercent), maxChangePercent);

            BigDecimal multiplier = BigDecimal.ONE.add(totalChangePercent.divide(HUNDRED, 18, RoundingMode.HALF_UP));
            BigDecimal newPrice = oldPrice.multiply(multiplier).setScale(18, RoundingMode.HALF_UP);
            if (newPrice.compareTo(minPrice) < 0) {
                newPrice = minPrice;
            }

            BigDecimal absChangeRatio = totalChangePercent.abs().divide(HUNDRED, 18, RoundingMode.HALF_UP);
            BigDecimal volumeFactor = randomBetween(volumeMinFactor, volumeMaxFactor);
            BigDecimal volume = coin.getCurrentSupply().multiply(absChangeRatio).multiply(volumeFactor).setScale(18, RoundingMode.HALF_UP);
            if (volume.compareTo(MIN_VOLUME) < 0) {
                volume = MIN_VOLUME;
            }

            coin.setCurrentPrice(newPrice);
            coin.setMarketCap(coin.getCurrentSupply().multiply(newPrice).setScale(18, RoundingMode.HALF_UP));

            historyRows.add(PriceHistory.builder()
                    .coin(coin)
                    .price(newPrice)
                    .volume(volume)
                    .recordedAt(Instant.now())
                    .build());

            publishSimulationEvent(coin.getSymbol(), volume, newPrice.subtract(oldPrice).abs());
            updated++;
        }

        if (updated == 0) {
            return;
        }

        coinRepository.saveAll(selected);
        priceHistoryRepository.saveAll(historyRows);

        log.info("market simulation tick applied: updatedCoins={}, regime={}, tickMs={}", updated, regime, tickMs);
    }

    private List<Coin> pickCoins(List<Coin> all) {
        if (maxCoinsPerTick <= 0 || all.size() <= maxCoinsPerTick) {
            return all;
        }

        List<Coin> copy = new ArrayList<>(all);
        java.util.Collections.shuffle(copy);
        return copy.subList(0, maxCoinsPerTick);
    }

    private MarketRegime drawRegime() {
        double p = ThreadLocalRandom.current().nextDouble();
        double shockChance = shockProbability.doubleValue();
        double crashThreshold = Math.max(0.01, shockChance * 0.35);
        double pumpThreshold = crashThreshold + Math.max(0.01, shockChance * 0.65);

        if (p < crashThreshold) {
            return MarketRegime.CRASH;
        }
        if (p < pumpThreshold) {
            return MarketRegime.PUMP;
        }

        double residual = 1.0 - pumpThreshold;
        double bearThreshold = pumpThreshold + residual * 0.2;
        double normalThreshold = bearThreshold + residual * 0.65;

        if (p < bearThreshold) {
            return MarketRegime.BEAR;
        }
        if (p < normalThreshold) {
            return MarketRegime.NORMAL;
        }
        return MarketRegime.BULL;
    }

    private BigDecimal drawRegimeShock(MarketRegime regime) {
        BigDecimal shockMagnitude = randomBetween(shockMinPercent, shockMaxPercent);
        BigDecimal randomSmall = randomBetween(baseVolatilityPercent.negate(), baseVolatilityPercent);

        return switch (regime) {
            case NORMAL -> randomSmall;
            case BULL -> shockMagnitude.multiply(new BigDecimal("0.35"));
            case BEAR -> shockMagnitude.multiply(new BigDecimal("-0.35"));
            case PUMP -> shockMagnitude;
            case CRASH -> shockMagnitude.negate();
        };
    }

    private BigDecimal randomBetween(BigDecimal min, BigDecimal max) {
        if (min.compareTo(max) >= 0) {
            return min;
        }

        double rand = ThreadLocalRandom.current().nextDouble();
        BigDecimal range = max.subtract(min);
        return min.add(range.multiply(BigDecimal.valueOf(rand))).setScale(18, RoundingMode.HALF_UP);
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal maxAbs) {
        BigDecimal floor = maxAbs.negate();
        if (value.compareTo(floor) < 0) {
            return floor;
        }
        if (value.compareTo(maxAbs) > 0) {
            return maxAbs;
        }
        return value;
    }

    private void publishSimulationEvent(String coinSymbol, BigDecimal amount, BigDecimal fee) {
        Map<String, Object> message = Map.of(
                "type", "TRADE",
                "fromUserId", "MARKET_SIM",
                "toUserId", "MARKET_SIM",
                "coinSymbol", coinSymbol,
                "amount", amount,
                "fee", fee
        );

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.MARKET_EXCHANGE,
            RabbitMQConfig.MARKET_SIMULATED_ROUTING_KEY,
            message
        );
    }

    private enum MarketRegime {
        NORMAL,
        BULL,
        BEAR,
        PUMP,
        CRASH
    }
}
