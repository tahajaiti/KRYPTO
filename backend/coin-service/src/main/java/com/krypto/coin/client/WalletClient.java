package com.krypto.coin.client;

import com.krypto.coin.client.dto.DebitKrypRequest;
import com.krypto.coin.client.dto.DebitKrypResponse;
import com.krypto.coin.client.dto.MintCoinRequest;
import com.krypto.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "wallet-service")
public interface WalletClient {

    @PostMapping("/api/wallets/{userId}/debit/kryp")
    ApiResponse<DebitKrypResponse> debitKryp(@PathVariable UUID userId,
                                             @RequestBody DebitKrypRequest request,
                                             @RequestHeader(value = "Cookie", required = false) String cookieHeader,
                                             @RequestHeader(value = "Authorization", required = false) String authorizationHeader);

    @PostMapping("/api/wallets/internal/coins/mint")
    ApiResponse<Void> mintCoin(@RequestBody MintCoinRequest request,
                               @RequestHeader("X-Internal-Secret") String internalSecret);
}
