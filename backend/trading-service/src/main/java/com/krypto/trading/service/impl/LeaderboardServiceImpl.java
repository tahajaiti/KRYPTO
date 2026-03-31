package com.krypto.trading.service.impl;

import com.krypto.common.dto.ApiResponse;
import com.krypto.trading.client.UserClient;
import com.krypto.trading.dto.response.LeaderboardEntryResponse;
import com.krypto.trading.entity.Trade;
import com.krypto.trading.repository.TradeRepository;
import com.krypto.trading.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardServiceImpl implements LeaderboardService {

    private final TradeRepository tradeRepository;
    private final UserClient userClient;

    @Override
    public List<LeaderboardEntryResponse> getLeaderboard(Integer limit) {
        int resolvedLimit = (limit == null || limit <= 0) ? 10 : limit;

        List<Trade> trades = tradeRepository.findAllByOrderByExecutedAtDesc();

        Map<UUID, LeaderboardAccumulator> stats = new HashMap<>();
        for (Trade trade : trades) {
            BigDecimal notional = trade.getPrice().multiply(trade.getAmount());
            
            accumulate(stats, trade.getBuyerId(), trade.getAmount(), notional);
            
            if (!trade.getBuyerId().equals(trade.getSellerId())) {
                accumulate(stats, trade.getSellerId(), trade.getAmount(), notional);
            }
        }

        Map<UUID, String> usernameMap = new HashMap<>();
        if (!stats.isEmpty()) {
            try {
                ApiResponse<Map<UUID, String>> response = userClient.lookupUsernames(stats.keySet());
                if (response != null && response.getData() != null) {
                    response.getData().forEach((k, v) -> {
                        Object keyObj = (Object) k;
                        if (keyObj instanceof String) {
                            usernameMap.put(UUID.fromString((String) keyObj), v);
                        } else {
                            usernameMap.put((UUID) keyObj, v);
                        }
                    });
                }
            } catch (Exception e) {
                log.warn("Failed to lookup usernames for leaderboard in LeaderboardService: {}", e.getMessage());
            }
        }

        List<LeaderboardEntryResponse> entries = new ArrayList<>();
        for (Map.Entry<UUID, LeaderboardAccumulator> entry : stats.entrySet()) {
            LeaderboardAccumulator value = entry.getValue();
            UUID userId = entry.getKey();
            entries.add(LeaderboardEntryResponse.builder()
                    .userId(userId)
                    .username(usernameMap.getOrDefault(userId, userId.toString().substring(0, 8)))
                    .totalVolume(value.totalVolume)
                    .totalNotional(value.totalNotional)
                    .trades(value.trades)
                    .build());
        }

        return entries.stream()
                .sorted(Comparator
                        .comparing(LeaderboardEntryResponse::getTotalNotional).reversed()
                        .thenComparing(LeaderboardEntryResponse::getTotalVolume).reversed())
                .limit(resolvedLimit)
                .collect(Collectors.toList()).reversed();
    }

    private void accumulate(Map<UUID, LeaderboardAccumulator> stats, UUID userId, BigDecimal volume, BigDecimal notional) {
        LeaderboardAccumulator item = stats.computeIfAbsent(userId, key -> new LeaderboardAccumulator());
        item.totalVolume = item.totalVolume.add(volume);
        item.totalNotional = item.totalNotional.add(notional);
        item.trades++;
    }

    private static class LeaderboardAccumulator {
        private BigDecimal totalVolume = BigDecimal.ZERO;
        private BigDecimal totalNotional = BigDecimal.ZERO;
        private long trades = 0;
    }
}
