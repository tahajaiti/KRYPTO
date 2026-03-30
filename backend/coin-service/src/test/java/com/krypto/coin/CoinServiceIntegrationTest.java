package com.krypto.coin;

import com.krypto.coin.client.WalletClient;
import com.krypto.coin.client.dto.DebitKrypResponse;
import com.krypto.coin.dto.request.CreateCoinRequest;
import com.krypto.coin.dto.response.CoinPriceResponse;
import com.krypto.coin.dto.response.CoinResponse;
import com.krypto.coin.service.CoinService;
import com.krypto.common.dto.ApiResponse;
import com.krypto.common.dto.PageResponse;
import com.krypto.common.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
class CoinServiceIntegrationTest {

    @Autowired
    private CoinService coinService;

    @MockitoBean
    private WalletClient walletClient;

    private UUID creatorId;

    @BeforeEach
    void setUp() {
        creatorId = UUID.randomUUID();
        authenticateAs(creatorId, "creator", "PLAYER");

        Mockito.when(walletClient.debitKryp(eq(creatorId), any(), any(), any()))
                .thenReturn(ApiResponse.ok(new DebitKrypResponse(null, "KRYP", new BigDecimal("9900"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateCoinListAndExposePrice() {
        CreateCoinRequest request = new CreateCoinRequest(
                "My Bitcoin",
                "mbtc",
                "https://example.com/mbtc.png",
                new BigDecimal("1000000"));

        HttpServletRequest httpRequest = new MockHttpServletRequest();
        CoinResponse created = coinService.createCoin(request, httpRequest);

        assertThat(created.getSymbol()).isEqualTo("MBTC");
        assertThat(created.getCreatorId()).isEqualTo(creatorId);

        PageResponse<CoinResponse> page = coinService.listCoins("mbt", true, PageRequest.of(0, 20));
        assertThat(page.getContent()).isNotEmpty();

        CoinPriceResponse price = coinService.getCoinPrice(created.getId());
        assertThat(price.getCoinId()).isEqualTo(created.getId());
        assertThat(price.getCurrentPrice()).isNotNull();
    }

    private void authenticateAs(UUID userId, String username, String role) {
        JwtPrincipal principal = new JwtPrincipal(userId, username, role);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
