package com.krypto.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final String accessTokenCookieName;
    private final boolean allowAuthorizationHeaderFallback;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            String accessTokenCookieName,
            boolean allowAuthorizationHeaderFallback
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.accessTokenCookieName = accessTokenCookieName;
        this.allowAuthorizationHeaderFallback = allowAuthorizationHeaderFallback;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Optional<String> token = extractFromCookies(request, accessTokenCookieName);

        if (token.isEmpty() && allowAuthorizationHeaderFallback) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = Optional.of(authHeader.substring(7));
            }
        }

        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null && jwtTokenProvider.isTokenValid(token.get())) {
                JwtPrincipal principal = jwtTokenProvider.extractPrincipal(token.get());
                String role = principal.role() != null ? principal.role() : "PLAYER";

                var authToken = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception ignored) {
            // invalid token, continue unauthenticated
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractFromCookies(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return Optional.of(value);
                }
            }
        }

        return Optional.empty();
    }
}
