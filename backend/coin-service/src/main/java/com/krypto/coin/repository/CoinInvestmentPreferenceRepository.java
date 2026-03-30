package com.krypto.coin.repository;

import com.krypto.coin.entity.CoinInvestmentPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoinInvestmentPreferenceRepository extends JpaRepository<CoinInvestmentPreference, UUID> {

    Optional<CoinInvestmentPreference> findByUserIdAndCoinId(UUID userId, UUID coinId);

    List<CoinInvestmentPreference> findByUserIdAndInvestingTrue(UUID userId);
}
