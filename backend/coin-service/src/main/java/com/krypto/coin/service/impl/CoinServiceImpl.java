package com.krypto.coin.service.impl;

import com.krypto.coin.client.WalletClient;
import com.krypto.coin.client.dto.DebitKrypRequest;
import com.krypto.coin.client.dto.MintCoinRequest;
import com.krypto.coin.dto.request.CreateCoinRequest;
import com.krypto.coin.dto.request.RecordTradeRequest;
import com.krypto.coin.dto.response.CoinPriceResponse;
import com.krypto.coin.dto.response.CoinResponse;
import com.krypto.coin.entity.Coin;
import com.krypto.coin.entity.PriceHistory;
import com.krypto.coin.mapper.CoinMapper;
import com.krypto.coin.repository.CoinRepository;
import com.krypto.coin.repository.PriceHistoryRepository;
import com.krypto.coin.service.CoinService;
import com.krypto.common.dto.PageResponse;
import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;
import com.krypto.common.exception.ResourceNotFoundException;
import com.krypto.common.security.AuthorizationUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoinServiceImpl implements CoinService {

    private final CoinRepository coinRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final CoinMapper coinMapper;
    private final WalletClient walletClient;

    @Value("${coin.creation-fee:100}")
    private BigDecimal creationFee;

    @Value("${krypto.internal-secret:krypto-internal-secret}")
    private String internalSecret;

    @Override
    @Transactional
    @CacheEvict(value = "coin-price", allEntries = true)
    public CoinResponse createCoin(CreateCoinRequest request, HttpServletRequest httpRequest) {
        String normalizedName = request.getName().trim();
        String normalizedSymbol = request.getSymbol().trim().toUpperCase(Locale.ROOT);

        if (normalizedName.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "name is required");
        }
        if (normalizedSymbol.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "symbol is required");
        }

        if (coinRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "coin name already exists");
        }
        if (coinRepository.existsBySymbolIgnoreCase(normalizedSymbol)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "coin symbol already exists");
        }

        UUID creatorId = AuthorizationUtils.requireUserId();
        debitCreationFee(creatorId, httpRequest);

        BigDecimal initialPrice = calculateInitialPrice(request.getInitialSupply());

        Coin coin = Coin.builder()
                .name(normalizedName)
                .symbol(normalizedSymbol)
                .image(request.getImage())
                .initialSupply(request.getInitialSupply())
                .currentSupply(request.getInitialSupply())
                .creatorId(creatorId)
                .creationFee(creationFee)
                .currentPrice(initialPrice)
                .marketCap(request.getInitialSupply().multiply(initialPrice))
                .active(true)
                .build();

        Coin saved = coinRepository.save(coin);

        PriceHistory firstPrice = PriceHistory.builder()
                .coin(saved)
                .price(saved.getCurrentPrice())
                .volume(BigDecimal.ZERO)
                .recordedAt(Instant.now())
                .build();
        priceHistoryRepository.save(firstPrice);

        mintInitialSupplyToCreator(saved);

        return coinMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CoinResponse getCoinById(UUID id) {
        Coin coin = coinRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coin", id));
        return coinMapper.toResponse(coin);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CoinResponse> listCoins(String query, Pageable pageable) {
        Page<Coin> page;
        if (query == null || query.isBlank()) {
            page = coinRepository.findByActiveTrue(pageable);
        } else {
            String term = query.trim();
            page = coinRepository.findByActiveTrueAndNameContainingIgnoreCaseOrActiveTrueAndSymbolContainingIgnoreCase(
                    term,
                    term,
                    pageable);
        }

        Sort.Order sortOrder = pageable.getSort().stream().findFirst().orElse(null);
        String sortBy = sortOrder != null ? sortOrder.getProperty() : null;
        String sortDirection = sortOrder != null ? sortOrder.getDirection().name() : null;

        return PageResponse.of(
                coinMapper.toResponseList(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getNumberOfElements(),
                sortBy,
                sortDirection);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "coin-price", key = "#id")
    public CoinPriceResponse getCoinPrice(UUID id) {
        Coin coin = coinRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coin", id));

        return CoinPriceResponse.builder()
                .coinId(coin.getId())
                .symbol(coin.getSymbol())
                .currentPrice(coin.getCurrentPrice())
                .build();
    }

    @Override
    @Transactional
    @CachePut(value = "coin-price", key = "#id")
    public CoinPriceResponse recordTrade(UUID id, RecordTradeRequest request, String providedSecret) {
        assertInternalSecret(providedSecret);

        Coin coin = coinRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coin", id));

        coin.setCurrentPrice(request.getPrice());
        coin.setMarketCap(coin.getCurrentSupply().multiply(request.getPrice()));
        Coin savedCoin = coinRepository.save(coin);

        PriceHistory priceHistory = PriceHistory.builder()
                .coin(savedCoin)
                .price(request.getPrice())
                .volume(request.getVolume())
                .recordedAt(Instant.now())
                .build();
        priceHistoryRepository.save(priceHistory);

        return CoinPriceResponse.builder()
                .coinId(savedCoin.getId())
                .symbol(savedCoin.getSymbol())
                .currentPrice(savedCoin.getCurrentPrice())
                .build();
    }

    private void debitCreationFee(UUID creatorId, HttpServletRequest httpRequest) {
        String cookieHeader = httpRequest.getHeader("Cookie");
        String authorizationHeader = httpRequest.getHeader("Authorization");

        try {
            walletClient.debitKryp(
                    creatorId,
                    new DebitKrypRequest(creationFee),
                    cookieHeader,
                    authorizationHeader);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "failed to charge coin creation fee", e);
        }
    }

    private void mintInitialSupplyToCreator(Coin coin) {
        try {
            walletClient.mintCoin(
                    new MintCoinRequest(
                            coin.getCreatorId(),
                            coin.getId(),
                            coin.getSymbol(),
                            coin.getInitialSupply()),
                    internalSecret);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "failed to mint initial supply to creator", e);
        }
    }

    private void assertInternalSecret(String providedSecret) {
        if (providedSecret == null || !providedSecret.equals(internalSecret)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "invalid internal secret");
        }
    }

    private BigDecimal calculateInitialPrice(BigDecimal initialSupply) {
        if (initialSupply.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "initialSupply must be positive");
        }

        BigDecimal raw = creationFee.divide(initialSupply, 18, RoundingMode.HALF_UP);
        BigDecimal min = new BigDecimal("0.0001");
        return raw.compareTo(min) < 0 ? min : raw;
    }
}
