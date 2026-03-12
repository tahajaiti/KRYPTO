package com.krypto.gamification.repository;

import com.krypto.gamification.entity.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserChallengeRepository extends JpaRepository<UserChallenge, UUID> {
    List<UserChallenge> findByUserIdAndCompletedFalse(String userId);
    List<UserChallenge> findByUserId(String userId);
    Optional<UserChallenge> findByUserIdAndChallengeId(String userId, UUID challengeId);
}
