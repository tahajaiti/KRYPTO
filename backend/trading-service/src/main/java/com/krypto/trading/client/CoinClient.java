package com.krypto.trading.client;

import com.krypto.common.dto.ApiResponse;
import com.krypto.trading.client.dto.CoinPriceResponse;
import com.krypto.trading.client.dto.RecordTradeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "coin-service")
public interface CoinClient {

    @GetMapping("/api/coins/{id}/price")
    ApiResponse<CoinPriceResponse> getCoinPrice(@PathVariable UUID id);

    @PostMapping("/api/coins/internal/{id}/trades")
    ApiResponse<CoinPriceResponse> recordTrade(@PathVariable UUID id,
                                               @RequestBody RecordTradeRequest request,
                                               @RequestHeader("X-Internal-Secret") String internalSecret);
}
