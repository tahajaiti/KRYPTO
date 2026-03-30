package com.krypto.user.service.impl;

import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;
import com.krypto.user.dto.request.UpdateProfileRequest;
import com.krypto.user.dto.response.UserResponse;
import com.krypto.user.entity.Role;
import com.krypto.user.entity.User;
import com.krypto.user.mapper.UserMapper;
import com.krypto.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldUpdateRoleForExistingUser() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("satoshi")
                .email("satoshi@krypto.com")
                .role(Role.PLAYER)
                .enabled(true)
                .password("encoded")
                .build();
        UserResponse mapped = UserResponse.builder().id(userId).username("satoshi").role(Role.ADMIN).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(mapped);

        UserResponse response = userService.updateRole(userId, Role.ADMIN);

        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
    verify(userRepository, times(1)).save(user);
    }

    @Test
    void shouldFailUpdateProfileWhenUsernameAlreadyTaken() {
        User existing = User.builder()
                .id(UUID.randomUUID())
                .username("current")
                .email("current@krypto.com")
                .password("encoded")
                .build();
        UpdateProfileRequest request = new UpdateProfileRequest("taken", "https://avatar");

        when(userRepository.findByUsername("current")).thenReturn(Optional.of(existing));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile("current", request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RESOURCE);
                    assertThat(be.getMessage()).isEqualTo("username already taken");
                });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void shouldReturnEmptySearchResultsWhenQueryIsTooShort() {
        var page = userService.searchUsers(" a ", PageRequest.of(0, 50));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getSortBy()).isEqualTo("username");
        assertThat(page.getSortDirection()).isEqualTo("ASC");
        verify(userRepository, never()).searchEnabledByUsernameOrEmail(any(String.class), any(Pageable.class));
    }
}