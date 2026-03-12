package com.krypto.gamification.service;

import com.krypto.gamification.dto.response.LeaderboardEntryResponse;

import java.util.List;

public interface GamificationService {
    List<LeaderboardEntryResponse> getLeaderboard(int limit);
    void processTradeEvent(String userId, Long notionalValue);
    void awardChallengeCompletionBadge(String userId, String challengeName);
}
