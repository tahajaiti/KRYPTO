package com.krypto.gamification.listener;

import com.krypto.gamification.event.CoinCreatedEvent;
import com.krypto.gamification.event.MarketSimulatedEvent;
import com.krypto.gamification.event.TradeExecutedEvent;
import com.krypto.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationEventListener {

    private final GamificationService gamificationService;

    @RabbitListener(queues = "gamification.trade.events.queue")
    public void onTradeExecuted(TradeExecutedEvent event) {
        log.info("Processing trade event for user {} with notional value {}", event.getUserId(), event.getNotionalValue());

        try {
            if (event.getUserId() != null && event.getNotionalValue() != null) {
                gamificationService.processTradeEvent(event.getUserId(), event.getQuantity(), event.getNotionalValue());
            }
        } catch (Exception e) {
            log.error("Error processing trade event", e);
        }
    }

    @RabbitListener(queues = "gamification.market.events.queue")
    public void onMarketSimulated(MarketSimulatedEvent event) {
        log.info("Processing market simulation event for coin {}", event.getCoinId());
    }

    @RabbitListener(queues = "gamification.coin.events.queue")
    public void onCoinCreated(CoinCreatedEvent event) {
        log.info("Processing coin created event for user {}", event.getUserId());

        try {
            if (event.getUserId() != null && !event.getUserId().isBlank()) {
                gamificationService.processCoinCreatedEvent(event.getUserId());
            }
        } catch (Exception e) {
            log.error("Error processing coin created event", e);
        }
    }
}
