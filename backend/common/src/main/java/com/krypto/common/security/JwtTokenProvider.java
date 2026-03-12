package com.krypto.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class JwtTokenProvider {

    private final String secret;
    private final long accessTokenExpiration;

    public JwtTokenProvider(String secret, long accessTokenExpiration) {
        this.secret = secret;
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String generateAccessToken(String username, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpiration)))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public JwtPrincipal extractPrincipal(String token) {
        Claims claims = extractAllClaims(token);
        String userIdValue = claims.get("userId", String.class);
        String roleValue = claims.get("role", String.class);

        UUID userId = userIdValue != null ? UUID.fromString(userIdValue) : null;
        return new JwtPrincipal(userId, claims.getSubject(), roleValue);
    }

    public boolean isTokenValid(String token, String username) {
        String tokenUsername = extractUsername(token);
        return tokenUsername.equals(username) && !isTokenExpired(token);
    }

    public boolean isTokenValid(String token) {
        extractAllClaims(token);
        return !isTokenExpired(token);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant().isBefore(Instant.now());
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
