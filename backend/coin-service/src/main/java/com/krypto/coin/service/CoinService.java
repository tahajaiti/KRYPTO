package com.krypto.coin.service;

import com.krypto.coin.dto.request.CreateCoinRequest;
import com.krypto.coin.dto.request.RecordTradeRequest;
import com.krypto.coin.dto.response.CoinInvestmentPreferenceResponse;
import com.krypto.coin.dto.response.CoinPriceHistoryPointResponse;
import com.krypto.coin.dto.response.CoinPriceResponse;
import com.krypto.coin.dto.response.CoinResponse;
import com.krypto.common.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CoinService {

    CoinResponse createCoin(CreateCoinRequest request, HttpServletRequest httpRequest);

    CoinResponse getCoinById(UUID id);

    PageResponse<CoinResponse> listCoins(String query, boolean activeOnly, Pageable pageable);

    CoinPriceResponse getCoinPrice(UUID id);

    Map<UUID, BigDecimal> getCoinPricesBatch(java.util.Set<UUID> coinIds);

    Map<UUID, CoinResponse> getCoinsBatch(java.util.Set<UUID> coinIds);

    List<CoinPriceHistoryPointResponse> getCoinPriceHistory(UUID id, Integer points, Instant from, Instant to);

    CoinInvestmentPreferenceResponse getInvestmentPreference(UUID coinId);

    CoinInvestmentPreferenceResponse updateInvestmentPreference(UUID coinId, boolean investing);

    List<CoinInvestmentPreferenceResponse> getMyInvestments();

    List<CoinResponse> getWatchedCoins(UUID userId);

    CoinPriceResponse recordTrade(UUID id, RecordTradeRequest request, String internalSecret);

    CoinResponse updateCoinStatus(UUID id, boolean active);

    PageResponse<CoinResponse> getByCreatorId(UUID creatorId, Pageable pageable);
}
