package com.krypto.blockchain.repository;

import com.krypto.blockchain.model.Block;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, UUID> {

    Optional<Block> findTopByOrderByIndexDesc();
}
