package com.krypto.user;

import com.krypto.common.dto.PageResponse;
import com.krypto.user.dto.request.RegisterRequest;
import com.krypto.user.dto.response.AuthResponse;
import com.krypto.user.dto.response.UserResponse;
import com.krypto.user.entity.Role;
import com.krypto.user.entity.User;
import com.krypto.user.repository.RefreshTokenRepository;
import com.krypto.user.repository.UserRepository;
import com.krypto.user.service.AuthService;
import com.krypto.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserAndFetchProfile() {
        RegisterRequest request = new RegisterRequest("satoshi", "satoshi@krypto.com", "nakamoto123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthResponse authResponse = authService.register(request, response);
        assertThat(authResponse.getUser().getUsername()).isEqualTo("satoshi");

        UserResponse current = userService.getCurrentUser("satoshi");
        assertThat(current.getEmail()).isEqualTo("satoshi@krypto.com");

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
    }

    @Test
    void shouldReturnPaginatedUsersForAdminFlow() {
        userRepository.save(User.builder()
                .username("admin")
                .email("admin@krypto.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        userRepository.save(User.builder()
                .username("user1")
                .email("user1@krypto.com")
                .password(passwordEncoder.encode("user123"))
                .role(Role.PLAYER)
                .enabled(true)
                .build());

        PageResponse<UserResponse> page = userService.getAllUsers(PageRequest.of(0, 20));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getPage()).isZero();
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
