package com.krypto.gamification.service.impl;

import com.krypto.gamification.dto.response.LeaderboardEntryResponse;
import com.krypto.gamification.entity.Challenge;
import com.krypto.gamification.repository.ChallengeRepository;
import com.krypto.gamification.repository.UserBadgeRepository;
import com.krypto.gamification.repository.UserChallengeRepository;
import com.krypto.gamification.service.BadgeService;
import com.krypto.gamification.service.ChallengeService;
import com.krypto.gamification.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationServiceImpl implements GamificationService {

    private final ChallengeRepository challengeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final BadgeService badgeService;
    private final ChallengeService challengeService;

    @Override
    @Cacheable(value = "gamification-leaderboard", key = "#limit", unless = "#result == null || #result.isEmpty()")
    public List<LeaderboardEntryResponse> getLeaderboard(int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 100));
        var challengesById = challengeRepository.findAll().stream()
                .collect(Collectors.toMap(Challenge::getId, challenge -> challenge));

        var accumulators = new HashMap<String, LeaderboardAccumulator>();

        userBadgeRepository.findAll().forEach(userBadge -> {
            var accumulator = accumulators.computeIfAbsent(userBadge.getUserId(), ignored -> new LeaderboardAccumulator());
            accumulator.addBadge();
        });

        userChallengeRepository.findAll().forEach(userChallenge -> {
            var challenge = challengesById.get(userChallenge.getChallengeId());
            var accumulator = accumulators.computeIfAbsent(userChallenge.getUserId(), ignored -> new LeaderboardAccumulator());

            if (challenge != null) {
                if (Boolean.TRUE.equals(userChallenge.getCompleted())) {
                    accumulator.addPoints(challenge.getRewardPoints());
                }

                var progress = userChallenge.getProgress() != null ? userChallenge.getProgress() : 0L;
                if (challenge.getMetric() == Challenge.ChallengeMetric.TOTAL_TRADES) {
                    accumulator.addTrades(progress);
                }
                if (challenge.getMetric() == Challenge.ChallengeMetric.TOTAL_NOTIONAL_VALUE
                        || challenge.getMetric() == Challenge.ChallengeMetric.TOTAL_VOLUME) {
                    accumulator.addNotionalValue(progress);
                }
            }
        });

        List<LeaderboardEntryResponse> sorted = accumulators.entrySet().stream()
                .sorted((a, b) -> {
                    int pointsCompare = Integer.compare(b.getValue().getTotalPoints(), a.getValue().getTotalPoints());
                    if (pointsCompare != 0) {
                        return pointsCompare;
                    }
                    return Integer.compare(b.getValue().getTotalBadges(), a.getValue().getTotalBadges());
                })
                .limit(cappedLimit)
                .map(entry -> LeaderboardEntryResponse.builder()
                        .userId(entry.getKey())
                        .totalBadges(entry.getValue().getTotalBadges())
                        .totalPoints(entry.getValue().getTotalPoints())
                        .totalTrades(entry.getValue().getTotalTrades())
                        .totalNotionalValue(entry.getValue().getTotalNotionalValue())
                        .build())
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setRank(i + 1);
        }

        return sorted;
    }

    @Override
    @CacheEvict(value = "gamification-leaderboard", allEntries = true)
    public void processTradeEvent(String userId, Long quantity, Long notionalValue) {
        log.info("Processing trade event for user {} quantity {} notional {}", userId, quantity, notionalValue);

        if (userId == null || userId.isBlank()) {
            return;
        }

        var safeQuantity = quantity != null && quantity > 0 ? quantity : 0L;
        var safeNotional = notionalValue != null && notionalValue > 0 ? notionalValue : 0L;

        processChallengesByMetric(userId, Challenge.ChallengeMetric.TOTAL_TRADES, 1L);
        processChallengesByMetric(userId, Challenge.ChallengeMetric.TOTAL_VOLUME, safeQuantity);
        processChallengesByMetric(userId, Challenge.ChallengeMetric.TOTAL_NOTIONAL_VALUE, safeNotional);
    }

    @Override
    @CacheEvict(value = "gamification-leaderboard", allEntries = true)
    public void processCoinCreatedEvent(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        processChallengesByMetric(userId, Challenge.ChallengeMetric.COINS_CREATED, 1L);
    }

    @Override
    @CacheEvict(value = "gamification-leaderboard", allEntries = true)
    public void processWalletValueSnapshot(String userId, Long walletValue) {
        if (userId == null || userId.isBlank() || walletValue == null || walletValue <= 0) {
            return;
        }

        challengeRepository.findByActiveTrue().stream()
                .filter(challenge -> isTypeProcessable(challenge.getType()))
                .filter(challenge -> challenge.getMetric() == Challenge.ChallengeMetric.WALLET_VALUE)
                .forEach(challenge -> {
                    var currentProgress = userChallengeRepository
                            .findByUserIdAndChallengeId(userId, challenge.getId())
                            .map(uc -> uc.getProgress() == null ? 0L : uc.getProgress())
                            .orElse(0L);

                    var delta = walletValue - currentProgress;
                    if (delta > 0) {
                        challengeService.updateUserChallengeProgress(userId, challenge.getId(), delta);
                    }
                });
    }

    @Override
    @CacheEvict(value = "gamification-leaderboard", allEntries = true)
    public void awardChallengeCompletionBadge(String userId, String challengeName) {
        if (userId == null || userId.isBlank() || challengeName == null || challengeName.isBlank()) {
            return;
        }

        challengeRepository.findByActiveTrue().stream()
                .filter(challenge -> challengeName.equalsIgnoreCase(challenge.getName()))
                .findFirst()
                .ifPresent(challenge -> {
                    if (challenge.getRewardBadgeId() != null) {
                        badgeService.awardBadgeToUser(userId, challenge.getRewardBadgeId());
                    }
                });
    }

    private void processChallengesByMetric(String userId, Challenge.ChallengeMetric metric, Long progressDelta) {
        if (progressDelta == null || progressDelta <= 0) {
            return;
        }

        challengeRepository.findByActiveTrue().stream()
                .filter(challenge -> isTypeProcessable(challenge.getType()))
                .filter(challenge -> challenge.getMetric() == metric)
                .forEach(challenge -> challengeService.updateUserChallengeProgress(userId, challenge.getId(), progressDelta));
    }

    private boolean isTypeProcessable(Challenge.ChallengeType type) {
        return type == Challenge.ChallengeType.DAILY
                || type == Challenge.ChallengeType.WEEKLY
                || type == Challenge.ChallengeType.SEASONAL;
    }

    private static class LeaderboardAccumulator {
        private int totalBadges = 0;
        private int totalPoints = 0;
        private long totalTrades = 0L;
        private long totalNotionalValue = 0L;

        void addBadge() {
            this.totalBadges++;
        }

        void addPoints(int points) {
            this.totalPoints += points;
        }

        void addTrades(long trades) {
            this.totalTrades += trades;
        }

        void addNotionalValue(long notionalValue) {
            this.totalNotionalValue += notionalValue;
        }

        int getTotalBadges() {
            return totalBadges;
        }

        int getTotalPoints() {
            return totalPoints;
        }

        long getTotalTrades() {
            return totalTrades;
        }

        long getTotalNotionalValue() {
            return totalNotionalValue;
        }
    }
}
