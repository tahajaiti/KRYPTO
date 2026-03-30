package com.krypto.coin.controller;

import com.krypto.coin.dto.response.CoinResponse;
import com.krypto.coin.service.CoinService;
import com.krypto.common.dto.ApiResponse;
import com.krypto.common.security.AuthorizationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/coins")
@RequiredArgsConstructor
public class AdminCoinController {

    private final CoinService coinService;

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<CoinResponse>> updateCoinStatus(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        AuthorizationUtils.requireRole("ADMIN");
        CoinResponse response = coinService.updateCoinStatus(id, active);
        return ResponseEntity.ok(ApiResponse.ok(response, active ? "coin activated" : "coin deactivated"));
    }
}
