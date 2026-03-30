package com.krypto.user.config;

import com.krypto.common.event.UserRegisteredEvent;
import com.krypto.user.entity.Role;
import com.krypto.user.entity.User;
import com.krypto.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void run(String... args) {
        seedUser("admin", "admin@admin.com", "admin123", Role.ADMIN,
                "https://media.craiyon.com/2023-10-05/87df0b0c18144755abeefed40bcfebf2.webp");

        log.info("User seeding complete. Only admin user active.");
    }

    private void seedUser(String username, String email, String rawPassword, Role role, String avatar) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        User user = userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .avatar(avatar)
                .enabled(true)
                .tutorialCompleted(true)
                .build());

        publishUserRegisteredEvent(user);
        log.info("Seeded user: {} ({})", username, role);
    }

    private void publishUserRegisteredEvent(User user) {
        try {
            UserRegisteredEvent event = UserRegisteredEvent.builder()
                    .userId(user.getId().toString())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();
            event.initialize("USER_REGISTERED");

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.USER_EXCHANGE,
                    RabbitMQConfig.USER_REGISTERED_ROUTING_KEY,
                    event);

            log.info("Published registration event for seeded user: {}", user.getUsername());
        } catch (Exception e) {
            log.warn("Could not publish registration event for {}: {}", user.getUsername(), e.getMessage());
        }
    }
}
