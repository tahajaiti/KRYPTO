package com.krypto.wallet.mapper;

import com.krypto.wallet.dto.response.BalanceItemResponse;
import com.krypto.wallet.dto.response.WalletResponse;
import com.krypto.wallet.entity.Wallet;
import com.krypto.wallet.entity.WalletBalance;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    WalletResponse toResponse(Wallet wallet);

    BalanceItemResponse toBalanceItemResponse(WalletBalance walletBalance);

    List<BalanceItemResponse> toBalanceItemResponses(List<WalletBalance> walletBalances);
}
