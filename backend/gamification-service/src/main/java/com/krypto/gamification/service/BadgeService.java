package com.krypto.gamification.service;

import com.krypto.gamification.dto.response.BadgeResponse;
import com.krypto.gamification.dto.response.UserBadgeResponse;

import java.util.List;
import java.util.UUID;

public interface BadgeService {
    List<BadgeResponse> getActiveBadges();
    BadgeResponse getBadgeById(UUID badgeId);
    List<UserBadgeResponse> getUserBadges(String userId);
    void awardBadgeToUser(String userId, UUID badgeId);
    boolean userHasBadge(String userId, UUID badgeId);
}
