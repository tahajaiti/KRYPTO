package com.krypto.gamification.service.impl;

import com.krypto.common.exception.ResourceNotFoundException;
import com.krypto.gamification.dto.response.ChallengeResponse;
import com.krypto.gamification.dto.response.UserChallengeResponse;
import com.krypto.gamification.entity.Challenge;
import com.krypto.gamification.entity.UserChallenge;
import com.krypto.gamification.repository.ChallengeRepository;
import com.krypto.gamification.repository.UserChallengeRepository;
import com.krypto.gamification.service.BadgeService;
import com.krypto.gamification.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final BadgeService badgeService;

    @Override
    public List<ChallengeResponse> getActiveChallenges() {
        return challengeRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChallengeResponse> getChallengesByType(Challenge.ChallengeType type) {
        return challengeRepository.findByTypeAndActiveTrue(type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ChallengeResponse getChallengeById(UUID challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found with id: " + challengeId));
        return mapToResponse(challenge);
    }

    @Override
    public List<UserChallengeResponse> getUserChallenges(String userId) {
        return userChallengeRepository.findByUserId(userId).stream()
                .map(this::mapUserChallengeToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserChallengeResponse getUserChallenge(String userId, UUID challengeId) {
        UserChallenge userChallenge = userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)
                .orElse(null);
        if (userChallenge == null) {
            // Validate challenge exists and auto-enroll user if not already enrolled.
            challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));
            userChallenge = UserChallenge.builder()
                    .userId(userId)
                    .challengeId(challengeId)
                    .progress(0L)
                    .completed(false)
                    .startedAt(System.currentTimeMillis())
                    .build();
            userChallenge = userChallengeRepository.save(userChallenge);
        }
        return mapUserChallengeToResponse(userChallenge);
    }

    @Override
    @Transactional
    public void updateUserChallengeProgress(String userId, UUID challengeId, Long progressDelta) {
        UserChallenge userChallenge = userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)
                .orElse(null);

        if (userChallenge == null) {
            Challenge challenge = challengeRepository.findById(challengeId).orElse(null);
            if (challenge == null) return;

            userChallenge = UserChallenge.builder()
                    .userId(userId)
                    .challengeId(challengeId)
                    .progress(progressDelta)
                    .completed(false)
                    .startedAt(System.currentTimeMillis())
                    .build();
        } else {
            userChallenge.setProgress(userChallenge.getProgress() + progressDelta);
        }

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));

        // Check if challenge is completed
        if (!userChallenge.getCompleted() && userChallenge.getProgress() >= challenge.getTargetValue()) {
            userChallenge.setCompleted(true);
            userChallenge.setCompletedAt(LocalDateTime.now());
            log.info("Challenge completed for user {} - challenge {}", userId, challengeId);

            if (challenge.getRewardBadgeId() != null) {
                try {
                    badgeService.awardBadgeToUser(userId, challenge.getRewardBadgeId());
                } catch (RuntimeException ex) {
                    // Badge award failures should not block challenge completion.
                    log.warn("Failed to award reward badge {} to user {}", challenge.getRewardBadgeId(), userId, ex);
                }
            }
        }

        userChallengeRepository.save(userChallenge);
    }

    private ChallengeResponse mapToResponse(Challenge challenge) {
        return ChallengeResponse.builder()
                .id(challenge.getId())
                .name(challenge.getName())
                .description(challenge.getDescription())
                .type(challenge.getType())
                .targetValue(challenge.getTargetValue())
                .rewardPoints(challenge.getRewardPoints())
                .rewardBadgeId(challenge.getRewardBadgeId())
                .metric(challenge.getMetric())
                .active(challenge.getActive())
                .build();
    }

    private UserChallengeResponse mapUserChallengeToResponse(UserChallenge userChallenge) {
        Challenge challenge = challengeRepository.findById(userChallenge.getChallengeId())
                .orElse(null);

        return UserChallengeResponse.builder()
                .id(userChallenge.getId())
                .challengeId(userChallenge.getChallengeId())
                .challengeName(challenge != null ? challenge.getName() : "Unknown")
                .progress(userChallenge.getProgress())
                .targetValue(challenge != null ? challenge.getTargetValue() : 0L)
                .completed(userChallenge.getCompleted())
                .completedAt(userChallenge.getCompletedAt())
                .rewardPoints(challenge != null ? challenge.getRewardPoints() : 0)
                .build();
    }
}
