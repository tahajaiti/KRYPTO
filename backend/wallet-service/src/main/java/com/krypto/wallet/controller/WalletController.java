package com.krypto.wallet.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.krypto.common.dto.ApiResponse;
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
import com.krypto.wallet.service.WalletService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getCurrentWallet() {
        WalletResponse response = walletService.getCurrentWallet();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getWalletByUserId(@PathVariable UUID userId) {
        WalletResponse response = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{userId}/balances")
    public ResponseEntity<ApiResponse<List<BalanceItemResponse>>> getBalances(@PathVariable UUID userId) {
        List<BalanceItemResponse> response = walletService.getBalances(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{userId}/balances/coin/{coinId}")
    public ResponseEntity<ApiResponse<BalanceItemResponse>> getBalanceByCoin(@PathVariable UUID userId, @PathVariable UUID coinId) {
        BalanceItemResponse response = walletService.getBalanceByCoin(userId, coinId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/internal/{userId}/balances/coin/{coinId}")
    public ResponseEntity<ApiResponse<BalanceItemResponse>> getBalanceByCoinInternal(
            @RequestHeader("X-Internal-Secret") String internalSecret,
            @PathVariable UUID userId, 
            @PathVariable UUID coinId
    ) {
        // walletService implementation already handles validation if needed or we can just call it
        // since /api/wallets/internal/** is permitAll() in SecurityConfig
        BalanceItemResponse response = walletService.getBalanceByCoin(userId, coinId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{userId}/balances/kryp")
    public ResponseEntity<ApiResponse<BalanceItemResponse>> getKrypBalance(@PathVariable UUID userId) {
        BalanceItemResponse response = walletService.getBalanceByCoin(userId, null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/internal/{userId}/balances/kryp")
    public ResponseEntity<ApiResponse<BalanceItemResponse>> getKrypBalanceInternal(
            @RequestHeader("X-Internal-Secret") String internalSecret,
            @PathVariable UUID userId
    ) {
        BalanceItemResponse response = walletService.getBalanceByCoin(userId, null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{userId}/debit/kryp")
    public ResponseEntity<ApiResponse<BalanceItemResponse>> debitKryp(@PathVariable UUID userId,
                                                                      @Valid @RequestBody DebitKrypRequest request) {
        BalanceItemResponse response = walletService.debitKryp(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "debit successful"));
    }

    @PostMapping("/internal/trades/settle")
    public ResponseEntity<ApiResponse<Void>> settleTrade(
            @RequestHeader("X-Internal-Secret") String internalSecret,
            @Valid @RequestBody SettleTradeRequest request
    ) {
        walletService.settleTradeInternal(request, internalSecret);
        return ResponseEntity.ok(ApiResponse.ok(null, "trade settled"));
    }

    @PostMapping("/internal/coins/mint")
    public ResponseEntity<ApiResponse<BalanceItemResponse>> mintCoin(
            @RequestHeader("X-Internal-Secret") String internalSecret,
            @Valid @RequestBody MintCoinRequest request
    ) {
        BalanceItemResponse response = walletService.mintCoinInternal(request, internalSecret);
        return ResponseEntity.ok(ApiResponse.ok(response, "coin minted"));
    }

    @GetMapping("/me/net-worth")
    public ResponseEntity<ApiResponse<NetWorthResponse>> getCurrentUserNetWorth() {
        NetWorthResponse response = walletService.getCurrentUserNetWorth();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{userId}/net-worth")
    public ResponseEntity<ApiResponse<NetWorthResponse>> getNetWorth(@PathVariable UUID userId) {
        NetWorthResponse response = walletService.getNetWorth(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/transfer/kryp")
    public ResponseEntity<ApiResponse<TransferResponse>> transferKryp(@Valid @RequestBody TransferKrypRequest request) {
        TransferResponse response = walletService.transferKryp(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "transfer successful"));
    }

    @GetMapping("/me/transfers")
    public ResponseEntity<PageResponse<WalletTransferItemResponse>> getCurrentUserTransferHistory(
            @PageableDefault(size = 50, sort = "transferredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<WalletTransferItemResponse> response = walletService.getCurrentUserTransferHistory(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
        return ResponseEntity.ok(response);
    }
}
