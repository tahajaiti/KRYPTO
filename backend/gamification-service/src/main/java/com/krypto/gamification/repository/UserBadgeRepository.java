package com.krypto.gamification.repository;

import com.krypto.gamification.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
    List<UserBadge> findByUserId(String userId);
    Optional<UserBadge> findByUserIdAndBadgeId(String userId, UUID badgeId);
    boolean existsByUserIdAndBadgeId(String userId, UUID badgeId);
}
