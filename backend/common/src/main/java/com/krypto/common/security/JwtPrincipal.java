package com.krypto.common.security;

import java.util.UUID;

public record JwtPrincipal(
        UUID userId,
        String username,
        String role
) {
}
