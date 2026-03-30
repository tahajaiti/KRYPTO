package com.krypto.coin.controller;

import com.krypto.coin.dto.request.CreateCoinRequest;
import com.krypto.coin.dto.request.RecordTradeRequest;
import com.krypto.coin.dto.response.CoinInvestmentPreferenceResponse;
import com.krypto.coin.dto.response.CoinPriceHistoryPointResponse;
import com.krypto.coin.dto.response.CoinPriceResponse;
import com.krypto.coin.dto.response.CoinResponse;
import com.krypto.coin.service.CoinService;
import com.krypto.common.dto.ApiResponse;
import com.krypto.common.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/coins")
@RequiredArgsConstructor
public class CoinController {

    private final CoinService coinService;

    @PostMapping
    public ResponseEntity<ApiResponse<CoinResponse>> createCoin(@Valid @RequestBody CreateCoinRequest request,
                                                                HttpServletRequest httpRequest) {
        CoinResponse response = coinService.createCoin(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "coin created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CoinResponse>> getCoinById(@PathVariable UUID id) {
        CoinResponse response = coinService.getCoinById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<PageResponse<CoinResponse>> listCoins(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<CoinResponse> response = coinService.listCoins(query, activeOnly, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/price")
    public ResponseEntity<ApiResponse<CoinPriceResponse>> getCoinPrice(@PathVariable UUID id) {
        CoinPriceResponse response = coinService.getCoinPrice(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/prices/batch")
    public ResponseEntity<ApiResponse<java.util.Map<UUID, java.math.BigDecimal>>> getCoinPricesBatch(@RequestBody java.util.Set<UUID> coinIds) {
        java.util.Map<UUID, java.math.BigDecimal> response = coinService.getCoinPricesBatch(coinIds);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<CoinPriceHistoryPointResponse>>> getCoinPriceHistory(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer points,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        List<CoinPriceHistoryPointResponse> response = coinService.getCoinPriceHistory(id, points, from, to);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/invest")
    public ResponseEntity<ApiResponse<CoinInvestmentPreferenceResponse>> getInvestmentPreference(@PathVariable UUID id) {
        CoinInvestmentPreferenceResponse response = coinService.getInvestmentPreference(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/invest")
    public ResponseEntity<ApiResponse<CoinInvestmentPreferenceResponse>> updateInvestmentPreference(
            @PathVariable UUID id,
            @RequestParam boolean investing
    ) {
        CoinInvestmentPreferenceResponse response = coinService.updateInvestmentPreference(id, investing);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/investments/me")
    public ResponseEntity<ApiResponse<List<CoinInvestmentPreferenceResponse>>> getMyInvestments() {
        List<CoinInvestmentPreferenceResponse> response = coinService.getMyInvestments();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/investments/user/{userId}/coins")
    public ResponseEntity<ApiResponse<List<CoinResponse>>> getWatchedCoins(@PathVariable UUID userId) {
        List<CoinResponse> response = coinService.getWatchedCoins(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/internal/{id}/trades")
    public ResponseEntity<ApiResponse<CoinPriceResponse>> recordTrade(@PathVariable UUID id,
                                                                      @RequestHeader("X-Internal-Secret") String internalSecret,
                                                                      @Valid @RequestBody RecordTradeRequest request) {
        CoinPriceResponse response = coinService.recordTrade(id, request, internalSecret);
        return ResponseEntity.ok(ApiResponse.ok(response, "trade recorded"));
    }

    @GetMapping("/creator/{creatorId}")
    public ResponseEntity<PageResponse<CoinResponse>> getCoinsByCreator(
            @PathVariable UUID creatorId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<CoinResponse> response = coinService.getByCreatorId(creatorId, pageable);
        return ResponseEntity.ok(response);
    }
}
