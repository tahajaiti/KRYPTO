package com.krypto.coin.service;

import com.krypto.coin.dto.request.CreateCoinRequest;
import com.krypto.coin.dto.request.RecordTradeRequest;
import com.krypto.coin.dto.response.CoinPriceResponse;
import com.krypto.coin.dto.response.CoinResponse;
import com.krypto.common.dto.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CoinService {

    CoinResponse createCoin(CreateCoinRequest request, HttpServletRequest httpRequest);

    CoinResponse getCoinById(UUID id);

    PageResponse<CoinResponse> listCoins(String query, Pageable pageable);

    CoinPriceResponse getCoinPrice(UUID id);

    CoinPriceResponse recordTrade(UUID id, RecordTradeRequest request, String internalSecret);
}
