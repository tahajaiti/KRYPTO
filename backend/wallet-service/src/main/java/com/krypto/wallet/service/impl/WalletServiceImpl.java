package com.krypto.wallet.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.krypto.common.dto.ApiResponse;
import com.krypto.common.dto.PageResponse;
import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;
import com.krypto.common.exception.ResourceNotFoundException;
import com.krypto.common.security.AuthorizationUtils;
import com.krypto.wallet.client.CoinClient;
import com.krypto.wallet.dto.request.DebitKrypRequest;
import com.krypto.wallet.dto.request.MintCoinRequest;
import com.krypto.wallet.dto.request.SettleTradeRequest;
import com.krypto.wallet.dto.request.TransferKrypRequest;
import com.krypto.wallet.dto.response.BalanceItemResponse;
import com.krypto.wallet.dto.response.NetWorthItemResponse;
import com.krypto.wallet.dto.response.NetWorthResponse;
import com.krypto.wallet.dto.response.TransferResponse;
import com.krypto.wallet.dto.response.WalletResponse;
import com.krypto.wallet.dto.response.WalletTransferItemResponse;
import com.krypto.wallet.entity.Wallet;
import com.krypto.wallet.entity.WalletBalance;
import com.krypto.wallet.entity.WalletTransfer;
import com.krypto.wallet.mapper.WalletMapper;
import com.krypto.wallet.repository.WalletBalanceRepository;
import com.krypto.wallet.repository.WalletRepository;
import com.krypto.wallet.repository.WalletTransferRepository;
import com.krypto.wallet.service.WalletService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class WalletServiceImpl implements WalletService {

    private static final String KRYP_SYMBOL = "KRYP";

    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final WalletTransferRepository walletTransferRepository;
    private final WalletMapper walletMapper;
    private final CoinClient coinClient;

    @Value("${krypto.initial-balance:10000}")
    private BigDecimal initialBalance;

    @Value("${krypto.internal-secret:krypto-internal-secret}")
    private String internalSecret;

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getCurrentWallet() {
        UUID userId = AuthorizationUtils.requireUserId();
        Wallet wallet = findWalletByUserId(userId);
        return walletMapper.toResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(UUID userId) {
        AuthorizationUtils.requireSelfOrRole(userId, "ADMIN");
        Wallet wallet = findWalletByUserId(userId);
        return walletMapper.toResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BalanceItemResponse> getBalances(UUID userId) {
        AuthorizationUtils.requireSelfOrRole(userId, "ADMIN");
        Wallet wallet = findWalletByUserId(userId);
        return walletMapper.toBalanceItemResponses(wallet.getBalances());
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceItemResponse getBalanceByCoin(UUID userId, UUID coinId) {
        AuthorizationUtils.requireSelfOrRole(userId, "ADMIN");
        Wallet wallet = findWalletByUserId(userId);
        
        WalletBalance balance;
        if (coinId == null) {
            balance = getOrCreateKrypBalance(wallet);
        } else {
            balance = walletBalanceRepository.findByWalletIdAndCoinId(wallet.getId(), coinId)
                    .orElseGet(() -> WalletBalance.builder()
                            .wallet(wallet)
                            .coinId(coinId)
                            .balance(BigDecimal.ZERO)
                            .build());
        }
        
        return walletMapper.toBalanceItemResponse(balance);
    }

    @Override
    @Transactional(readOnly = true)
    public NetWorthResponse getCurrentUserNetWorth() {
        UUID userId = AuthorizationUtils.requireUserId();
        return getNetWorthInternal(userId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public NetWorthResponse getNetWorth(UUID userId) {
        return getNetWorthInternal(userId, false);
    }

    @Override
    @Transactional
    public BalanceItemResponse debitKryp(UUID userId, DebitKrypRequest request) {
        AuthorizationUtils.requireSelfOrRole(userId, "ADMIN");

        Wallet wallet = findWalletByUserId(userId);
        WalletBalance krypBalance = getOrCreateKrypBalance(wallet);

        if (krypBalance.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "insufficient KRYP balance");
        }

        krypBalance.setBalance(krypBalance.getBalance().subtract(request.getAmount()));
        WalletBalance saved = walletBalanceRepository.save(krypBalance);
        return walletMapper.toBalanceItemResponse(saved);
    }

    @Override
    @Transactional
    public void settleTradeInternal(SettleTradeRequest request, String providedSecret) {
        assertInternalSecret(providedSecret);

        Wallet buyerWallet = findWalletByUserId(request.getBuyerId());
        Wallet sellerWallet = findWalletByUserId(request.getSellerId());

        WalletBalance buyerKryp = getOrCreateKrypBalance(buyerWallet);
        WalletBalance sellerKryp = getOrCreateKrypBalance(sellerWallet);
        WalletBalance buyerCoin = getOrCreateCoinBalance(buyerWallet, request.getCoinId(), request.getCoinSymbol());
        WalletBalance sellerCoin = getOrCreateCoinBalance(sellerWallet, request.getCoinId(), request.getCoinSymbol());

        BigDecimal notional = request.getPrice().multiply(request.getAmount());
        BigDecimal buyerCost = notional.add(request.getFee());

        if (buyerKryp.getBalance().compareTo(buyerCost) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "buyer has insufficient KRYP");
        }

        if (sellerCoin.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "seller has insufficient coin balance");
        }

        buyerKryp.setBalance(buyerKryp.getBalance().subtract(buyerCost));
        sellerKryp.setBalance(sellerKryp.getBalance().add(notional));
        sellerCoin.setBalance(sellerCoin.getBalance().subtract(request.getAmount()));
        buyerCoin.setBalance(buyerCoin.getBalance().add(request.getAmount()));

        walletBalanceRepository.save(buyerKryp);
        walletBalanceRepository.save(sellerKryp);
        walletBalanceRepository.save(sellerCoin);
        walletBalanceRepository.save(buyerCoin);
    }

    @Override
    @Transactional
    public BalanceItemResponse mintCoinInternal(MintCoinRequest request, String providedSecret) {
        assertInternalSecret(providedSecret);

        Wallet wallet = findWalletByUserId(request.getUserId());
        WalletBalance coinBalance = getOrCreateCoinBalance(wallet, request.getCoinId(), request.getSymbol());
        coinBalance.setBalance(coinBalance.getBalance().add(request.getAmount()));

        return walletMapper.toBalanceItemResponse(walletBalanceRepository.save(coinBalance));
    }

    @Override
    @Transactional
    public TransferResponse transferKryp(TransferKrypRequest request) {
        UUID fromUserId = AuthorizationUtils.requireUserId();

        if (fromUserId.equals(request.getToUserId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "cannot transfer to yourself");
        }

        Wallet fromWallet = findWalletByUserId(fromUserId);
        Wallet toWallet = findWalletByUserId(request.getToUserId());

        WalletBalance fromKryp = getOrCreateKrypBalance(fromWallet);
        WalletBalance toKryp = getOrCreateKrypBalance(toWallet);

        if (fromKryp.getBalance().compareTo(request.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, "insufficient KRYP balance");
        }

        fromKryp.setBalance(fromKryp.getBalance().subtract(request.getAmount()));
        toKryp.setBalance(toKryp.getBalance().add(request.getAmount()));

        walletBalanceRepository.save(fromKryp);
        walletBalanceRepository.save(toKryp);

        WalletTransfer transfer = walletTransferRepository.save(
            WalletTransfer.builder()
                .fromUserId(fromUserId)
                .toUserId(request.getToUserId())
                .amount(request.getAmount())
                .transferredAt(Instant.now())
                .build()
        );

        return TransferResponse.builder()
                .fromUserId(fromUserId)
                .toUserId(request.getToUserId())
                .amount(request.getAmount())
            .transferredAt(transfer.getTransferredAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WalletTransferItemResponse> getCurrentUserTransferHistory(int page, int size) {
        UUID userId = AuthorizationUtils.requireUserId();
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.clamp(size, 1, 200);

        Page<WalletTransfer> transferPage = walletTransferRepository.findByFromUserIdOrToUserId(
                userId,
                userId,
                PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(Sort.Direction.DESC, "transferredAt"))
        );

        List<WalletTransferItemResponse> content = transferPage
                .getContent()
                .stream()
                .map(transfer -> WalletTransferItemResponse.builder()
                        .id(transfer.getId())
                        .fromUserId(transfer.getFromUserId())
                        .toUserId(transfer.getToUserId())
                        .amount(transfer.getAmount())
                        .transferredAt(transfer.getTransferredAt())
                        .build())
                .toList();

        return PageResponse.of(
                content,
                transferPage.getNumber(),
                transferPage.getSize(),
                transferPage.getTotalElements(),
                transferPage.getNumberOfElements(),
                "transferredAt",
                "DESC"
        );
    }

    @Override
    @Transactional
    public void createWalletForUser(UUID userId) {
        if (walletRepository.existsByUserId(userId)) {
            return;
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        WalletBalance krypBalance = WalletBalance.builder()
                .wallet(savedWallet)
                .coinId(null)
                .symbol(KRYP_SYMBOL)
                .balance(initialBalance)
                .build();

        walletBalanceRepository.save(krypBalance);
    }

    private Wallet findWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId));
    }

    private NetWorthResponse getNetWorthInternal(UUID userId, boolean currentUser) {
        if (!currentUser) {
            AuthorizationUtils.requireSelfOrRole(userId, "ADMIN");
        }

        Wallet wallet = findWalletByUserId(userId);
        List<WalletBalance> balances = wallet.getBalances();

        java.util.Set<UUID> coinIds = balances.stream()
                .map(WalletBalance::getCoinId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<UUID, BigDecimal> priceMap = new java.util.HashMap<>();
        if (!coinIds.isEmpty()) {
            try {
                ApiResponse<java.util.Map<UUID, BigDecimal>> response = coinClient.getCoinPricesBatch(coinIds);
                if (response != null && response.getData() != null) {
                    for (java.util.Map.Entry<?, BigDecimal> entry : response.getData().entrySet()) {
                        UUID key;
                        Object k = entry.getKey();
                        if (k instanceof String) {
                            key = UUID.fromString((String) k);
                        } else if (k instanceof UUID) {
                            key = (UUID) k;
                        } else {
                            continue;
                        }
                        priceMap.put(key, entry.getValue());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch batch coin prices for net worth calculation: {}", e.getMessage());
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        List<NetWorthItemResponse> breakdown = new java.util.ArrayList<>();

        for (WalletBalance balance : balances) {
            BigDecimal priceInKryp;
            boolean isKryp = balance.getSymbol() != null && KRYP_SYMBOL.equalsIgnoreCase(balance.getSymbol());

            if (isKryp) {
                priceInKryp = BigDecimal.ONE;
            } else if (balance.getCoinId() != null) {
                priceInKryp = priceMap.getOrDefault(balance.getCoinId(), BigDecimal.ZERO);
            } else {
                priceInKryp = BigDecimal.ZERO;
            }

            BigDecimal valueInKryp = balance.getBalance().multiply(priceInKryp);
            total = total.add(valueInKryp);

            breakdown.add(NetWorthItemResponse.builder()
                    .coinId(balance.getCoinId())
                    .symbol(balance.getSymbol())
                    .balance(balance.getBalance())
                    .priceInKryp(priceInKryp)
                    .valueInKryp(valueInKryp)
                    .build());
        }

        log.info("Calculated net worth for user {}: total={} KRYP (from {} assets)", userId, total, breakdown.size());

        return NetWorthResponse.builder()
                .userId(userId)
                .totalNetWorthInKryp(total)
                .breakdown(breakdown)
                .build();
    }

    private WalletBalance getOrCreateKrypBalance(Wallet wallet) {
        return walletBalanceRepository.findByWalletIdAndSymbol(wallet.getId(), KRYP_SYMBOL)
                .orElseGet(() -> walletBalanceRepository.save(
                        WalletBalance.builder()
                                .wallet(wallet)
                                .coinId(null)
                                .symbol(KRYP_SYMBOL)
                                .balance(BigDecimal.ZERO)
                                .build()
                ));
    }

    private WalletBalance getOrCreateCoinBalance(Wallet wallet, UUID coinId, String symbol) {
        return walletBalanceRepository.findByWalletIdAndCoinId(wallet.getId(), coinId)
                .orElseGet(() -> walletBalanceRepository.save(
                        WalletBalance.builder()
                                .wallet(wallet)
                                .coinId(coinId)
                                .symbol(symbol)
                                .balance(BigDecimal.ZERO)
                                .build()
                ));
    }

    private void assertInternalSecret(String providedSecret) {
        if (providedSecret == null || !providedSecret.equals(internalSecret)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "invalid internal secret");
        }
    }
}
