package com.krypto.user.controller;

import com.krypto.common.dto.ApiResponse;
import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;
import com.krypto.user.dto.request.LoginRequest;
import com.krypto.user.dto.request.RegisterRequest;
import com.krypto.user.dto.response.AuthResponse;
import com.krypto.user.security.CookieService;
import com.krypto.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieService cookieService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request, response);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(authResponse, "registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.ok(authResponse, "login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(HttpServletRequest request,
                                                             HttpServletResponse response) {
        String refreshToken = cookieService.extractToken(request, CookieService.REFRESH_TOKEN_COOKIE)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "refresh token not found"));
        AuthResponse authResponse = authService.refresh(refreshToken, response);
        return ResponseEntity.ok(ApiResponse.ok(authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request,
                                                    HttpServletResponse response) {
        String refreshToken = cookieService.extractToken(request, CookieService.REFRESH_TOKEN_COOKIE)
                .orElse(null);
        authService.logout(refreshToken, response);
        return ResponseEntity.ok(ApiResponse.ok(null, "logout successful"));
    }
}
