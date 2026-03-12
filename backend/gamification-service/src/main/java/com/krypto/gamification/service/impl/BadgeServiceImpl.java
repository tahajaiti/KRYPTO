package com.krypto.gamification.service.impl;

import com.krypto.common.exception.ResourceNotFoundException;
import com.krypto.gamification.dto.response.BadgeResponse;
import com.krypto.gamification.dto.response.UserBadgeResponse;
import com.krypto.gamification.entity.Badge;
import com.krypto.gamification.entity.UserBadge;
import com.krypto.gamification.repository.BadgeRepository;
import com.krypto.gamification.repository.UserBadgeRepository;
import com.krypto.gamification.service.BadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    @Override
    public List<BadgeResponse> getActiveBadges() {
        return badgeRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BadgeResponse getBadgeById(UUID badgeId) {
        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Badge not found with id: " + badgeId));
        return mapToResponse(badge);
    }

    @Override
    public List<UserBadgeResponse> getUserBadges(String userId) {
        return userBadgeRepository.findByUserId(userId).stream()
                .map(this::mapUserBadgeToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void awardBadgeToUser(String userId, UUID badgeId) {
        // Check if user already has this badge
        if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)) {
            log.debug("User {} already has badge {}", userId, badgeId);
            return;
        }

        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Badge not found with id: " + badgeId));

        UserBadge userBadge = UserBadge.builder()
                .userId(userId)
                .badgeId(badgeId)
                .awardedAt(System.currentTimeMillis())
                .build();

        userBadgeRepository.save(userBadge);
        log.info("Badge {} awarded to user {}", badge.getName(), userId);
    }

    @Override
    public boolean userHasBadge(String userId, UUID badgeId) {
        return userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId);
    }

    private BadgeResponse mapToResponse(Badge badge) {
        return BadgeResponse.builder()
                .id(badge.getId())
                .name(badge.getName())
                .description(badge.getDescription())
                .icon(badge.getIcon())
                .points(badge.getPoints())
                .tier(badge.getTier())
                .active(badge.getActive())
                .build();
    }

    private UserBadgeResponse mapUserBadgeToResponse(UserBadge userBadge) {
        Badge badge = badgeRepository.findById(userBadge.getBadgeId())
                .orElse(null);

        return UserBadgeResponse.builder()
                .id(userBadge.getId())
                .badgeId(userBadge.getBadgeId())
                .badgeName(badge != null ? badge.getName() : "Unknown")
                .icon(badge != null ? badge.getIcon() : "")
                .points(badge != null ? badge.getPoints() : 0)
                .awardedAt(userBadge.getAwardedAt())
                .build();
    }
}
