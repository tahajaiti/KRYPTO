package com.krypto.blockchain.service;

import com.krypto.blockchain.dto.request.AddTransactionRequest;
import com.krypto.blockchain.dto.response.BlockResponse;
import com.krypto.blockchain.dto.response.ChainValidationResponse;
import com.krypto.blockchain.dto.response.TransactionResponse;
import com.krypto.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface BlockchainService {

    TransactionResponse addTransaction(AddTransactionRequest request);

    BlockResponse minePendingTransactions();

    BlockResponse getLatestBlock();

    PageResponse<BlockResponse> getBlocks(Pageable pageable);

    ChainValidationResponse verifyChain();
}
