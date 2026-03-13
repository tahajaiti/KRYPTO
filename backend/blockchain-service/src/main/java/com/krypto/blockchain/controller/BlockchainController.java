package com.krypto.blockchain.controller;

import com.krypto.blockchain.dto.request.AddTransactionRequest;
import com.krypto.blockchain.dto.response.BlockResponse;
import com.krypto.blockchain.dto.response.ChainValidationResponse;
import com.krypto.blockchain.dto.response.TransactionResponse;
import com.krypto.blockchain.service.BlockchainService;
import com.krypto.common.dto.ApiResponse;
import com.krypto.common.dto.PageResponse;
import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;
import com.krypto.common.security.AuthorizationUtils;
import com.krypto.common.security.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blockchain")
@RequiredArgsConstructor
public class BlockchainController {

    private final BlockchainService blockchainService;

    @PostMapping("/transactions")
    public ResponseEntity<ApiResponse<TransactionResponse>> addTransaction(
            Authentication authentication,
            @Valid @RequestBody AddTransactionRequest request
    ) {
        JwtPrincipal principal = AuthorizationUtils.requirePrincipal();
        String callerUserId = principal.userId() != null ? principal.userId().toString() : null;

        if (callerUserId != null && !callerUserId.isBlank()) {
            if (request.getFromUserId() == null || request.getFromUserId().isBlank()) {
                request.setFromUserId(callerUserId);
            } else if (!request.getFromUserId().equals(callerUserId) && !AuthorizationUtils.hasRole("ADMIN")) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "cannot submit transaction for another user");
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(blockchainService.addTransaction(request)));
    }

    @PostMapping("/mine")
    public ResponseEntity<ApiResponse<BlockResponse>> minePendingTransactions(Authentication authentication) {
        AuthorizationUtils.requireRole("ADMIN");
        return ResponseEntity.ok(ApiResponse.ok(blockchainService.minePendingTransactions()));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<BlockResponse>> getLatestBlock(Authentication authentication) {
        AuthorizationUtils.requirePrincipal();
        return ResponseEntity.ok(ApiResponse.ok(blockchainService.getLatestBlock()));
    }

    @GetMapping
    public ResponseEntity<PageResponse<BlockResponse>> getBlocks(Authentication authentication, Pageable pageable) {
        AuthorizationUtils.requirePrincipal();
        return ResponseEntity.ok(blockchainService.getBlocks(pageable));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<ChainValidationResponse>> verifyChain(Authentication authentication) {
        AuthorizationUtils.requireRole("ADMIN");
        return ResponseEntity.ok(ApiResponse.ok(blockchainService.verifyChain()));
    }
}
