package com.krypto.common.security;

import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class AuthorizationUtils {

    private AuthorizationUtils() {
    }

    public static Optional<JwtPrincipal> getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return Optional.of(jwtPrincipal);
        }

        return Optional.empty();
    }

    public static JwtPrincipal requirePrincipal() {
        return getCurrentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "authentication required"));
    }

    public static UUID requireUserId() {
        JwtPrincipal principal = requirePrincipal();
        if (principal.userId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "user id missing from token");
        }
        return principal.userId();
    }

    public static String requireUsername() {
        JwtPrincipal principal = requirePrincipal();
        if (principal.username() == null || principal.username().isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "username missing from token");
        }
        return principal.username();
    }

    public static boolean hasRole(String role) {
        String normalizedExpected = normalizeRole(role);
        JwtPrincipal principal = requirePrincipal();
        String normalizedActual = normalizeRole(principal.role());
        return normalizedActual.equals(normalizedExpected);
    }

    public static void requireRole(String role) {
        if (!hasRole(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "insufficient permissions");
        }
    }

    public static void requireSelfOrRole(UUID userId, String role) {
        UUID currentUserId = requireUserId();
        if (!currentUserId.equals(userId) && !hasRole(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "insufficient permissions");
        }
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            return normalized.substring(5);
        }

        return normalized;
    }
}
