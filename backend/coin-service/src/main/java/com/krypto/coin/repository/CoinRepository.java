package com.krypto.coin.repository;

import com.krypto.coin.entity.Coin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoinRepository extends JpaRepository<Coin, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySymbolIgnoreCase(String symbol);

    Optional<Coin> findByIdAndActiveTrue(UUID id);

    List<Coin> findByActiveTrue();

    Page<Coin> findByActiveTrue(Pageable pageable);

    Page<Coin> findByActiveTrueAndNameContainingIgnoreCaseOrActiveTrueAndSymbolContainingIgnoreCase(
            String name,
            String symbol,
            Pageable pageable
    );

    Page<Coin> findByCreatorIdAndActiveTrue(UUID creatorId, Pageable pageable);

    Page<Coin> findByCreatorId(UUID creatorId, Pageable pageable);

    Page<Coin> findByNameContainingIgnoreCaseOrSymbolContainingIgnoreCase(String name, String symbol, Pageable pageable);
}
