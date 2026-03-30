package com.krypto.blockchain.repository;

import com.krypto.blockchain.model.ChainTransaction;
import com.krypto.blockchain.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChainTransactionRepository extends JpaRepository<ChainTransaction, UUID> {

    Optional<ChainTransaction> findTopBySourceEventId(String sourceEventId);

    long countByStatus(TransactionStatus status);

    List<ChainTransaction> findByStatusOrderByTimestampAscIdAsc(TransactionStatus status);
}
