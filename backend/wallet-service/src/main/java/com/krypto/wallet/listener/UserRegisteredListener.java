package com.krypto.wallet.listener;

import com.krypto.common.event.UserRegisteredEvent;
import com.krypto.wallet.config.RabbitMQConfig;
import com.krypto.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredListener {

    private final WalletService walletService;

    @RabbitListener(queues = RabbitMQConfig.USER_REGISTERED_QUEUE)
    public void handleUserRegistered(UserRegisteredEvent event) {
        try {
            UUID userId = UUID.fromString(event.getUserId());
            walletService.createWalletForUser(userId);
            log.info("wallet initialized for user: {}", event.getUserId());
        } catch (Exception e) {
            log.error("failed to initialize wallet for user: {}", event.getUserId(), e);
        }
    }
}
