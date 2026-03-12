package com.krypto.wallet.client;

import com.krypto.common.dto.ApiResponse;
import com.krypto.wallet.client.dto.CoinPriceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "coin-service")
public interface CoinClient {

    @GetMapping("/api/coins/{coinId}/price")
    ApiResponse<CoinPriceResponse> getCoinPrice(@PathVariable UUID coinId);
}
