package com.krypto.trading.client;

import com.krypto.common.dto.ApiResponse;
import com.krypto.trading.client.dto.SettleTradeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.UUID;
import com.krypto.trading.client.dto.BalanceItemResponse;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "wallet-service")
public interface WalletClient {

    @PostMapping("/api/wallets/internal/trades/settle")
    ApiResponse<Void> settleTrade(@RequestBody SettleTradeRequest request,
                                  @RequestHeader("X-Internal-Secret") String internalSecret);

    @GetMapping("/api/wallets/internal/{userId}/balances/coin/{coinId}")
    ApiResponse<BalanceItemResponse> getCoinBalance(@PathVariable("userId") UUID userId,
                                                    @PathVariable("coinId") UUID coinId,
                                                    @RequestHeader("X-Internal-Secret") String internalSecret);

    @GetMapping("/api/wallets/internal/{userId}/balances/kryp")
    ApiResponse<BalanceItemResponse> getKrypBalance(@PathVariable("userId") UUID userId,
                                                    @RequestHeader("X-Internal-Secret") String internalSecret);
}
