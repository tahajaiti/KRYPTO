package com.krypto.coin.repository;

import com.krypto.coin.entity.PriceHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

	List<PriceHistory> findByCoinIdOrderByRecordedAtDesc(UUID coinId, Pageable pageable);

	List<PriceHistory> findByCoinIdAndRecordedAtBetweenOrderByRecordedAtAsc(UUID coinId, Instant from, Instant to);
}
