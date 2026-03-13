package com.krypto.gamification;

import com.krypto.gamification.dto.response.BadgeResponse;
import com.krypto.gamification.dto.response.ChallengeResponse;
import com.krypto.gamification.dto.response.LeaderboardEntryResponse;
import com.krypto.gamification.entity.Badge;
import com.krypto.gamification.entity.Challenge;
import com.krypto.gamification.repository.BadgeRepository;
import com.krypto.gamification.repository.ChallengeRepository;
import com.krypto.gamification.repository.UserBadgeRepository;
import com.krypto.gamification.repository.UserChallengeRepository;
import com.krypto.gamification.service.BadgeService;
import com.krypto.gamification.service.ChallengeService;
import com.krypto.gamification.service.GamificationService;
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

    @Autowired
    private GamificationService gamificationService;

    @Autowired
    private UserBadgeRepository userBadgeRepository;

    @Autowired
    private UserChallengeRepository userChallengeRepository;

    private Challenge dailyChallenge;
    private Badge bronzeBadge;
    private Challenge weeklyVolumeChallenge;
    private Challenge seasonalNotionalChallenge;
    private Challenge coinCreationChallenge;
    private Challenge walletValueChallenge;

    @BeforeEach
    public void setup() {
        userBadgeRepository.deleteAll();
        userChallengeRepository.deleteAll();
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

            weeklyVolumeChallenge = challengeRepository.save(
                Challenge.builder()
                    .name("Volume Runner")
                    .description("Reach required volume")
                    .type(Challenge.ChallengeType.WEEKLY)
                    .metric(Challenge.ChallengeMetric.TOTAL_VOLUME)
                    .targetValue(50L)
                    .rewardPoints(20)
                    .active(true)
                    .build()
            );

            seasonalNotionalChallenge = challengeRepository.save(
                Challenge.builder()
                    .name("Notional Whale")
                    .description("Reach notional threshold")
                    .type(Challenge.ChallengeType.SEASONAL)
                    .metric(Challenge.ChallengeMetric.TOTAL_NOTIONAL_VALUE)
                    .targetValue(200L)
                    .rewardPoints(30)
                    .active(true)
                    .build()
            );

            coinCreationChallenge = challengeRepository.save(
                Challenge.builder()
                    .name("Creator")
                    .description("Create one coin")
                    .type(Challenge.ChallengeType.DAILY)
                    .metric(Challenge.ChallengeMetric.COINS_CREATED)
                    .targetValue(1L)
                    .rewardPoints(15)
                    .active(true)
                    .build()
            );

            walletValueChallenge = challengeRepository.save(
                Challenge.builder()
                    .name("Wallet Milestone")
                    .description("Reach wallet value threshold")
                    .type(Challenge.ChallengeType.WEEKLY)
                    .metric(Challenge.ChallengeMetric.WALLET_VALUE)
                    .targetValue(500L)
                    .rewardPoints(25)
                    .active(true)
                    .build()
            );
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

    @Test
    public void shouldProcessTradeEventForAllTradeDrivenMetricsAndTypes() {
        String userId = "trade-user-1";

        gamificationService.processTradeEvent(userId, 25L, 100L);
        gamificationService.processTradeEvent(userId, 25L, 100L);

        var tradesChallenge = challengeService.getUserChallenge(userId, dailyChallenge.getId());
        var volumeChallenge = challengeService.getUserChallenge(userId, weeklyVolumeChallenge.getId());
        var notionalChallenge = challengeService.getUserChallenge(userId, seasonalNotionalChallenge.getId());

        assertTrue(tradesChallenge.getCompleted());
        assertTrue(volumeChallenge.getCompleted());
        assertTrue(notionalChallenge.getCompleted());
    }

    @Test
    public void shouldProcessCoinCreatedMetric() {
        String userId = "creator-user-1";

        gamificationService.processCoinCreatedEvent(userId);

        var challenge = challengeService.getUserChallenge(userId, coinCreationChallenge.getId());
        assertTrue(challenge.getCompleted());
    }

    @Test
    public void shouldProcessWalletValueSnapshotAsMaxProgress() {
        String userId = "wallet-user-1";

        gamificationService.processWalletValueSnapshot(userId, 300L);
        var challengeAfterFirst = challengeService.getUserChallenge(userId, walletValueChallenge.getId());
        assertFalse(challengeAfterFirst.getCompleted());

        gamificationService.processWalletValueSnapshot(userId, 700L);
        var challengeAfterSecond = challengeService.getUserChallenge(userId, walletValueChallenge.getId());
        assertTrue(challengeAfterSecond.getCompleted());

        gamificationService.processWalletValueSnapshot(userId, 200L);
        var challengeAfterLowerSnapshot = challengeService.getUserChallenge(userId, walletValueChallenge.getId());
        assertTrue(challengeAfterLowerSnapshot.getProgress() >= 700L);
    }

    @Test
    public void shouldReturnRankedLeaderboard() {
        String userA = "leaderboard-user-a";
        String userB = "leaderboard-user-b";

        gamificationService.processTradeEvent(userA, 50L, 300L);
        gamificationService.processCoinCreatedEvent(userA);

        gamificationService.processTradeEvent(userB, 10L, 20L);

        List<LeaderboardEntryResponse> leaderboard = gamificationService.getLeaderboard(10);

        assertFalse(leaderboard.isEmpty());
        assertNotNull(leaderboard.get(0).getRank());
        assertEquals(1, leaderboard.get(0).getRank());
        assertTrue(leaderboard.stream().anyMatch(entry -> entry.getUserId().equals(userA)));
        assertTrue(leaderboard.stream().anyMatch(entry -> entry.getUserId().equals(userB)));
    }
}
