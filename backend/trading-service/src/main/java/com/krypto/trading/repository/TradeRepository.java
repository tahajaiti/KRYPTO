package com.krypto.trading.repository;

import com.krypto.trading.entity.Trade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {

    Page<Trade> findByBuyerIdOrSellerIdOrderByExecutedAtDesc(UUID buyerId, UUID sellerId, Pageable pageable);

    Page<Trade> findByCoinIdOrderByExecutedAtDesc(UUID coinId, Pageable pageable);

    java.util.List<Trade> findAllByOrderByExecutedAtDesc();
}
