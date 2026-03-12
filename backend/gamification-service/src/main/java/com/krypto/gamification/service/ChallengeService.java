package com.krypto.gamification.service;

import com.krypto.gamification.dto.response.ChallengeResponse;
import com.krypto.gamification.dto.response.UserChallengeResponse;
import com.krypto.gamification.entity.Challenge;

import java.util.List;
import java.util.UUID;

public interface ChallengeService {
    List<ChallengeResponse> getActiveChallenges();
    List<ChallengeResponse> getChallengesByType(Challenge.ChallengeType type);
    ChallengeResponse getChallengeById(UUID challengeId);
    List<UserChallengeResponse> getUserChallenges(String userId);
    UserChallengeResponse getUserChallenge(String userId, UUID challengeId);
    void updateUserChallengeProgress(String userId, UUID challengeId, Long progressDelta);
}
