package com.krypto.trading.service;

import com.krypto.trading.dto.response.LeaderboardEntryResponse;
import java.util.List;

/**
 * Service for managing trading leaderboards.
 */
public interface LeaderboardService {

    /**
     * Retrieves the current trading leaderboard, ranked by total notional volume.
     *
     * @param limit The maximum number of entries to return.
     * @return A list of leaderboard entries.
     */
    List<LeaderboardEntryResponse> getLeaderboard(Integer limit);
}
