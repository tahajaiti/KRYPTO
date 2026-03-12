package com.krypto.wallet.repository;

import com.krypto.wallet.entity.WalletBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletBalanceRepository extends JpaRepository<WalletBalance, UUID> {

    Optional<WalletBalance> findByWalletIdAndSymbol(UUID walletId, String symbol);

    Optional<WalletBalance> findByWalletIdAndCoinId(UUID walletId, UUID coinId);
}
