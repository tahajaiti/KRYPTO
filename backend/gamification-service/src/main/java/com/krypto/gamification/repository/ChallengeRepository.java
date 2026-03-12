package com.krypto.gamification.repository;

import com.krypto.gamification.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {
    List<Challenge> findByTypeAndActiveTrue(Challenge.ChallengeType type);
    List<Challenge> findByActiveTrue();
}
