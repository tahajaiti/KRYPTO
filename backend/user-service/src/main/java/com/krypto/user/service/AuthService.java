package com.krypto.user.service;

import com.krypto.user.dto.request.LoginRequest;
import com.krypto.user.dto.request.RegisterRequest;
import com.krypto.user.dto.response.AuthResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request, HttpServletResponse response);

    AuthResponse login(LoginRequest request, HttpServletResponse response);

    AuthResponse refresh(String refreshTokenValue, HttpServletResponse response);

    void logout(String refreshTokenValue, HttpServletResponse response);
}
