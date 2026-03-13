package com.krypto.gamification.config;

import com.krypto.gamification.dto.response.*;
import com.krypto.gamification.entity.Badge;
import com.krypto.gamification.entity.Challenge;
import com.krypto.gamification.entity.UserBadge;
import com.krypto.gamification.entity.UserChallenge;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GamificationMapper {
    ChallengeResponse challengeToChallengeResponse(Challenge challenge);
    BadgeResponse badgeToBadgeResponse(Badge badge);

    @Mapping(target = "challengeName", ignore = true)
    @Mapping(target = "targetValue", ignore = true)
    @Mapping(target = "rewardPoints", ignore = true)
    UserChallengeResponse userChallengeToUserChallengeResponse(UserChallenge userChallenge);

    @Mapping(target = "badgeName", ignore = true)
    @Mapping(target = "icon", ignore = true)
    @Mapping(target = "points", ignore = true)
    UserBadgeResponse userBadgeToUserBadgeResponse(UserBadge userBadge);
}
