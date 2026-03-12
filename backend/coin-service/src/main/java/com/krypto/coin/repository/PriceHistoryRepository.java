package com.krypto.coin.repository;

import com.krypto.coin.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {
}
