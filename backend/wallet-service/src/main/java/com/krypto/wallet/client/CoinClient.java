package com.krypto.wallet.client;

import com.krypto.common.dto.ApiResponse;
import com.krypto.wallet.client.dto.CoinDetailResponse;
import com.krypto.wallet.client.dto.CoinPriceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@FeignClient(name = "coin-service")
public interface CoinClient {

    @GetMapping("/api/coins/{coinId}/price")
    ApiResponse<CoinPriceResponse> getCoinPrice(@PathVariable UUID coinId);

    @PostMapping("/api/coins/prices/batch")
    ApiResponse<Map<UUID, BigDecimal>> getCoinPricesBatch(@RequestBody Set<UUID> coinIds);

    @PostMapping("/api/coins/batch")
    ApiResponse<Map<UUID, CoinDetailResponse>> getCoinsBatch(@RequestBody Set<UUID> coinIds);
}
