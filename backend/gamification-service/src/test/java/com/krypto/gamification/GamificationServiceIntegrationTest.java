package com.krypto.gamification;

import com.krypto.gamification.dto.response.BadgeResponse;
import com.krypto.gamification.dto.response.ChallengeResponse;
import com.krypto.gamification.entity.Badge;
import com.krypto.gamification.entity.Challenge;
import com.krypto.gamification.repository.BadgeRepository;
import com.krypto.gamification.repository.ChallengeRepository;
import com.krypto.gamification.service.BadgeService;
import com.krypto.gamification.service.ChallengeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class GamificationServiceIntegrationTest {

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private ChallengeService challengeService;

    @Autowired
    private BadgeService badgeService;

    private Challenge dailyChallenge;
    private Badge bronzeBadge;

    @BeforeEach
    public void setup() {
        badgeRepository.deleteAll();
        challengeRepository.deleteAll();

        // Create a test badge
        bronzeBadge = Badge.builder()
                .name("Bronze Trader")
                .description("Awarded for 10 successful trades")
            .icon("bronze-medal")
                .points(5)
                .tier(Badge.BadgeTier.BRONZE)
                .active(true)
                .build();
            bronzeBadge = badgeRepository.save(bronzeBadge);

            // Create a test challenge
            dailyChallenge = Challenge.builder()
                .name("First Trade")
                .description("Complete your first trade")
                .type(Challenge.ChallengeType.DAILY)
                .metric(Challenge.ChallengeMetric.TOTAL_TRADES)
                .targetValue(1L)
                .rewardPoints(10)
                .rewardBadgeId(bronzeBadge.getId())
                .active(true)
                .build();
            dailyChallenge = challengeRepository.save(dailyChallenge);
    }

    @Test
    public void shouldCreateAndRetrieveChallenges() {
        List<ChallengeResponse> challenges = challengeService.getActiveChallenges();
        assertFalse(challenges.isEmpty());
    }

    @Test
    public void shouldGetChallengesByType() {
        List<ChallengeResponse> dailyChallenges = challengeService.getChallengesByType(Challenge.ChallengeType.DAILY);
        assertFalse(dailyChallenges.isEmpty());
    }

    @Test
    public void shouldRetrieveBadges() {
        List<BadgeResponse> badges = badgeService.getActiveBadges();
        assertFalse(badges.isEmpty());
    }

    @Test
    public void shouldAwardBadgeToUser() {
        String userId = "test-user-123";
        badgeService.awardBadgeToUser(userId, bronzeBadge.getId());
        assertTrue(badgeService.userHasBadge(userId, bronzeBadge.getId()));
    }

    @Test
    public void shouldUpdateUserChallengeProgress() {
        String userId = "test-user-123";
        challengeService.updateUserChallengeProgress(userId, dailyChallenge.getId(), 1L);
        var userChallenge = challengeService.getUserChallenge(userId, dailyChallenge.getId());
        assertTrue(userChallenge.getCompleted());
        assertTrue(badgeService.userHasBadge(userId, bronzeBadge.getId()));
    }
}
