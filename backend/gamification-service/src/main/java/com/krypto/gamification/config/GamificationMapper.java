package com.krypto.gamification.config;

import com.krypto.gamification.dto.response.*;
import com.krypto.gamification.entity.Badge;
import com.krypto.gamification.entity.Challenge;
import com.krypto.gamification.entity.UserBadge;
import com.krypto.gamification.entity.UserChallenge;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GamificationMapper {
    ChallengeResponse challengeToChallengeResponse(Challenge challenge);
    BadgeResponse badgeToBadgeResponse(Badge badge);
    UserChallengeResponse userChallengeToUserChallengeResponse(UserChallenge userChallenge);
    UserBadgeResponse userBadgeToUserBadgeResponse(UserBadge userBadge);
}
