package com.krypto.trading.client;

import com.krypto.common.dto.ApiResponse;
import com.krypto.trading.client.dto.SettleTradeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "wallet-service")
public interface WalletClient {

    @PostMapping("/api/wallets/internal/trades/settle")
    ApiResponse<Void> settleTrade(@RequestBody SettleTradeRequest request,
                                  @RequestHeader("X-Internal-Secret") String internalSecret);
}
