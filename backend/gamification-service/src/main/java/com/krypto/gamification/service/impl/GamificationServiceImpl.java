package com.krypto.gamification.service.impl;

import com.krypto.gamification.dto.response.LeaderboardEntryResponse;
import com.krypto.gamification.repository.UserBadgeRepository;
import com.krypto.gamification.repository.UserChallengeRepository;
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

    private final UserBadgeRepository userBadgeRepository;
    private final UserChallengeRepository userChallengeRepository;

    @Override
    @Cacheable(value = "gamification-leaderboard", key = "#limit", unless = "#result == null || #result.isEmpty()")
    public List<LeaderboardEntryResponse> getLeaderboard(int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 100));

        // Collect all users with their badges
        Set<String> allUserIds = new HashSet<>();
        userBadgeRepository.findAll().forEach(ub -> allUserIds.add(ub.getUserId()));
        userChallengeRepository.findAll().forEach(uc -> allUserIds.add(uc.getUserId()));

        // Build leaderboard entries
        Map<String, LeaderboardAccumulator> accumulators = new HashMap<>();

        userBadgeRepository.findAll().forEach(userBadge -> {
            accumulators.computeIfAbsent(userBadge.getUserId(), k -> new LeaderboardAccumulator())
                    .addBadge();
        });

        userChallengeRepository.findAll().stream()
                .filter(uc -> uc.getCompleted())
                .forEach(userChallenge -> {
                    accumulators.computeIfAbsent(userChallenge.getUserId(), k -> new LeaderboardAccumulator())
                            .addPoints(10); // Default challenge completion points
                });

        // Sort and paginate
        List<LeaderboardEntryResponse> sorted = accumulators.entrySet().stream()
                .sorted((a, b) -> {
                    int pointsCompare = Integer.compare(b.getValue().getTotalPoints(), a.getValue().getTotalPoints());
                    if (pointsCompare != 0) return pointsCompare;
                    return Integer.compare(b.getValue().getTotalBadges(), a.getValue().getTotalBadges());
                })
                .limit(cappedLimit)
                .map(entry -> LeaderboardEntryResponse.builder()
                        .userId(entry.getKey())
                        .totalBadges(entry.getValue().getTotalBadges())
                        .totalPoints(entry.getValue().getTotalPoints())
                        .totalTrades(0L) // Placeholder - would need trading service integration
                        .totalNotionalValue(0L) // Placeholder - would need trading service integration
                        .build())
                .collect(Collectors.toList());

        // Add rank
        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setRank(i + 1);
        }

        return sorted;
    }

    @Override
    @CacheEvict(value = "gamification-leaderboard", allEntries = true)
    public void processTradeEvent(String userId, Long notionalValue) {
        log.info("Processing trade event for user {} with notional value {}", userId, notionalValue);
        // This would be called by the RabbitMQ listener when a trade is executed
        // Updates would be handled by the business logic
    }

    @Override
    public void awardChallengeCompletionBadge(String userId, String challengeName) {
        log.info("Awarding challenge completion badge to user {} for challenge {}", userId, challengeName);
        // This would be called when a user completes a challenge
        // The actual badge award is handled by ChallengeServiceImpl
    }

    private static class LeaderboardAccumulator {
        private int totalBadges = 0;
        private int totalPoints = 0;

        void addBadge() {
            this.totalBadges++;
            this.totalPoints += 5; // Badge worth 5 points
        }

        void addPoints(int points) {
            this.totalPoints += points;
        }

        int getTotalBadges() {
            return totalBadges;
        }

        int getTotalPoints() {
            return totalPoints;
        }
    }
}
