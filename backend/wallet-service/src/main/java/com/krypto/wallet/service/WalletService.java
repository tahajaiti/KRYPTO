package com.krypto.wallet.service;

import java.util.List;
import java.util.UUID;

import com.krypto.common.dto.PageResponse;
import com.krypto.wallet.dto.request.DebitKrypRequest;
import com.krypto.wallet.dto.request.MintCoinRequest;
import com.krypto.wallet.dto.request.SettleTradeRequest;
import com.krypto.wallet.dto.request.TransferKrypRequest;
import com.krypto.wallet.dto.response.BalanceItemResponse;
import com.krypto.wallet.dto.response.NetWorthResponse;
import com.krypto.wallet.dto.response.TransferResponse;
import com.krypto.wallet.dto.response.WalletResponse;
import com.krypto.wallet.dto.response.WalletTransferItemResponse;

public interface WalletService {

    WalletResponse getCurrentWallet();

    WalletResponse getWalletByUserId(UUID userId);

    List<BalanceItemResponse> getBalances(UUID userId);
    
    BalanceItemResponse getBalanceByCoin(UUID userId, UUID coinId);

    NetWorthResponse getCurrentUserNetWorth();

    NetWorthResponse getNetWorth(UUID userId);

    BalanceItemResponse debitKryp(UUID userId, DebitKrypRequest request);

    void settleTradeInternal(SettleTradeRequest request, String internalSecret);

    BalanceItemResponse mintCoinInternal(MintCoinRequest request, String internalSecret);

    TransferResponse transferKryp(TransferKrypRequest request);

    PageResponse<WalletTransferItemResponse> getCurrentUserTransferHistory(int page, int size);

    void createWalletForUser(UUID userId);
}
