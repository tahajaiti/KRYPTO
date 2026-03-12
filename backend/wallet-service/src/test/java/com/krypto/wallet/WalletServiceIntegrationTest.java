package com.krypto.wallet;

import com.krypto.common.security.JwtPrincipal;
import com.krypto.wallet.dto.request.DebitKrypRequest;
import com.krypto.wallet.dto.request.TransferKrypRequest;
import com.krypto.wallet.dto.response.NetWorthResponse;
import com.krypto.wallet.dto.response.TransferResponse;
import com.krypto.wallet.repository.WalletBalanceRepository;
import com.krypto.wallet.repository.WalletRepository;
import com.krypto.wallet.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WalletServiceIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletBalanceRepository walletBalanceRepository;

    private UUID user1;
    private UUID user2;

    @BeforeEach
    void setUp() {
        walletBalanceRepository.deleteAll();
        walletRepository.deleteAll();

        user1 = UUID.randomUUID();
        user2 = UUID.randomUUID();

        walletService.createWalletForUser(user1);
        walletService.createWalletForUser(user2);

        authenticateAs(user1, "user1", "PLAYER");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldTransferKrypAndReturnNetWorth() {
        TransferResponse transfer = walletService.transferKryp(new TransferKrypRequest(user2, new BigDecimal("250")));
        assertThat(transfer.getAmount()).isEqualByComparingTo("250");

        NetWorthResponse netWorth = walletService.getCurrentUserNetWorth();
        assertThat(netWorth.getUserId()).isEqualTo(user1);
        assertThat(netWorth.getTotalNetWorthInKryp()).isEqualByComparingTo("9750");
    }

    @Test
    void shouldAllowSelfDebitOperation() {
        var balance = walletService.debitKryp(user1, new DebitKrypRequest(new BigDecimal("100")));
        assertThat(balance.getSymbol()).isEqualTo("KRYP");
        assertThat(balance.getBalance()).isEqualByComparingTo("9900");
    }

    private void authenticateAs(UUID userId, String username, String role) {
        JwtPrincipal principal = new JwtPrincipal(userId, username, role);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
