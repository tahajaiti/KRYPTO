package com.krypto.gamification.listener;

import com.krypto.gamification.event.TradeExecutedEvent;
import com.krypto.gamification.service.ChallengeService;
import com.krypto.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationEventListener {

    private final ChallengeService challengeService;
    private final GamificationService gamificationService;

    @RabbitListener(queues = "gamification.events.queue")
    public void onTradeExecuted(TradeExecutedEvent event) {
        log.info("Processing trade event for user {} with notional value {}", event.getUserId(), event.getNotionalValue());

        try {
            if (event.getUserId() != null && event.getNotionalValue() != null) {
                // Update trade-related challenges
                // Challenge ID for "Total Trading Volume" - example UUID
                try {
                    challengeService.updateUserChallengeProgress(
                            event.getUserId(),
                            UUID.fromString("00000000-0000-0000-0000-000000000002"),
                            event.getNotionalValue()
                    );
                } catch (Exception e) {
                    log.debug("Could not update challenge progress", e);
                }

                // Process gamification event
                gamificationService.processTradeEvent(event.getUserId(), event.getNotionalValue());
            }
        } catch (Exception e) {
            log.error("Error processing trade event", e);
        }
    }
}
