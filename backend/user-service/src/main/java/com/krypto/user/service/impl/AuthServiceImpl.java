package com.krypto.user.service.impl;

import com.krypto.common.event.UserRegisteredEvent;
import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;
import com.krypto.common.security.JwtTokenProvider;
import com.krypto.user.config.RabbitMQConfig;
import com.krypto.user.dto.request.LoginRequest;
import com.krypto.user.dto.request.RegisterRequest;
import com.krypto.user.dto.response.AuthResponse;
import com.krypto.user.entity.RefreshToken;
import com.krypto.user.entity.User;
import com.krypto.user.mapper.UserMapper;
import com.krypto.user.repository.RefreshTokenRepository;
import com.krypto.user.repository.UserRepository;
import com.krypto.user.security.CookieService;
import com.krypto.user.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieService cookieService;
    private final AuthenticationManager authenticationManager;
    private final RabbitTemplate rabbitTemplate;
    private final UserMapper userMapper;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "email already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);

        publishUserRegisteredEvent(user);
        setAuthCookies(user, response);

        return AuthResponse.builder()
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword()));

        User user = userRepository.findByUsername(request.getLogin())
                .or(() -> userRepository.findByEmail(request.getLogin()))
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "invalid credentials"));

        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "account is disabled");
        }

        setAuthCookies(user, response);

        return AuthResponse.builder()
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refresh(String refreshTokenValue, HttpServletResponse response) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refresh token expired");
        }

        User user = refreshToken.getUser();

        // rotate refresh token on each use
        refreshTokenRepository.delete(refreshToken);
        setAuthCookies(user, response);

        return AuthResponse.builder()
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshTokenValue, HttpServletResponse response) {
        if (refreshTokenValue != null) {
            refreshTokenRepository.findByToken(refreshTokenValue)
                    .ifPresent(refreshTokenRepository::delete);
        }
        cookieService.clearAuthCookies(response);
    }

    private void setAuthCookies(User user, HttpServletResponse response) {
        Map<String, Object> claims = Map.of(
                "userId", user.getId().toString(),
                "role", user.getRole().name()
        );

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), claims);
        String refreshToken = createRefreshToken(user).getToken();

        cookieService.addAccessTokenCookie(response, accessToken);
        cookieService.addRefreshTokenCookie(response, refreshToken);
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiration))
                .build();

        return refreshTokenRepository.save(token);
    }

    private void publishUserRegisteredEvent(User user) {
        try {
            UserRegisteredEvent event = UserRegisteredEvent.builder()
                    .userId(user.getId().toString())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build();
            event.initialize("USER_REGISTERED");

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.USER_EXCHANGE,
                    RabbitMQConfig.USER_REGISTERED_ROUTING_KEY,
                    event);

            log.info("published user registered event for user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("failed to publish user registered event for user: {}", user.getUsername(), e);
        }
    }
}
