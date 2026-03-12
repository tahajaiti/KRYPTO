package com.krypto.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetWorthResponse {

    private UUID userId;
    private BigDecimal totalNetWorthInKryp;
    private List<NetWorthItemResponse> breakdown;
}
