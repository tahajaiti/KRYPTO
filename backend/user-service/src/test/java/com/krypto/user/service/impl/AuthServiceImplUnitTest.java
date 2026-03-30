package com.krypto.user.service.impl;

import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;
import com.krypto.common.security.JwtTokenProvider;
import com.krypto.user.dto.request.RegisterRequest;
import com.krypto.user.dto.response.UserResponse;
import com.krypto.user.entity.RefreshToken;
import com.krypto.user.entity.Role;
import com.krypto.user.entity.User;
import com.krypto.user.mapper.UserMapper;
import com.krypto.user.repository.RefreshTokenRepository;
import com.krypto.user.repository.UserRepository;
import com.krypto.user.security.CookieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CookieService cookieService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);
    }

    @Test
    void shouldRegisterUserAndSetAuthCookies() {
        RegisterRequest request = new RegisterRequest("satoshi", "satoshi@krypto.com", "nakamoto123");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .username("satoshi")
                .email("satoshi@krypto.com")
                .password("encoded-password")
                .role(Role.PLAYER)
                .enabled(true)
                .build();

        UserResponse mapped = UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .enabled(true)
                .build();

        when(userRepository.existsByUsername("satoshi")).thenReturn(false);
        when(userRepository.existsByEmail("satoshi@krypto.com")).thenReturn(false);
        when(passwordEncoder.encode("nakamoto123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessToken(eq("satoshi"), anyMap())).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(savedUser)).thenReturn(mapped);

        var result = authService.register(request, response);

        assertThat(result.getUser().getUsername()).isEqualTo("satoshi");
        verify(cookieService).addAccessTokenCookie(response, "access-token");
        verify(cookieService).addRefreshTokenCookie(eq(response), any(String.class));
        verify(rabbitTemplate).convertAndSend(eq("user.exchange"), eq("user.registered"), any(Object.class));
    }

    @Test
    void shouldFailRegistrationWhenUsernameAlreadyExists() {
        RegisterRequest request = new RegisterRequest("satoshi", "satoshi@krypto.com", "nakamoto123");

        when(userRepository.existsByUsername("satoshi")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, new org.springframework.mock.web.MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
                    assertThat(be.getMessage()).isEqualTo("username already taken");
                });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldDeleteExpiredRefreshTokenAndThrowUnauthorized() {
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("satoshi")
                .email("satoshi@krypto.com")
                .password("encoded")
                .build();
        RefreshToken expired = RefreshToken.builder()
                .token("expired-token")
                .user(user)
                .expiresAt(Instant.now().minusSeconds(60))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refresh("expired-token", response))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                    assertThat(be.getMessage()).isEqualTo("refresh token expired");
                });

        verify(refreshTokenRepository).delete(expired);
        verify(cookieService, never()).addAccessTokenCookie(eq(response), any(String.class));
    }
}