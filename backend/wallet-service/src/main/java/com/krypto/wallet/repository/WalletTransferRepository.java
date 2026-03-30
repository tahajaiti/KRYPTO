package com.krypto.wallet.repository;

import com.krypto.wallet.entity.WalletTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletTransferRepository extends JpaRepository<WalletTransfer, UUID> {

    Page<WalletTransfer> findByFromUserIdOrToUserId(UUID fromUserId, UUID toUserId, Pageable pageable);
}
