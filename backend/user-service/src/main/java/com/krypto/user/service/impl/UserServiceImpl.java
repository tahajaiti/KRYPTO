package com.krypto.user.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.krypto.common.dto.PageResponse;
import com.krypto.common.exception.BusinessException;
import com.krypto.common.exception.ErrorCode;
import com.krypto.common.exception.ResourceNotFoundException;
import com.krypto.user.dto.request.UpdateProfileRequest;
import com.krypto.user.dto.response.UserLookupResponse;
import com.krypto.user.dto.response.UserResponse;
import com.krypto.user.entity.Role;
import com.krypto.user.entity.User;
import com.krypto.user.mapper.UserMapper;
import com.krypto.user.repository.UserRepository;
import com.krypto.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getCurrentUser(String username) {
        User user = findByUsername(username);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse getUserByUsername(String username) {
        User user = findByUsername(username);
        return userMapper.toResponse(user);
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);

        Sort.Order sortOrder = pageable.getSort().stream().findFirst().orElse(null);
        String sortBy = sortOrder != null ? sortOrder.getProperty() : null;
        String sortDirection = sortOrder != null ? sortOrder.getDirection().name() : null;

        return PageResponse.of(
                userMapper.toResponseList(page.getContent()),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getNumberOfElements(),
                sortBy,
                sortDirection);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = findByUsername(username);

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "username already taken");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse completeTutorial(String username) {
        User user = findByUsername(username);
        user.setTutorialCompleted(true);
        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateRole(UUID id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setRole(role);
        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(UUID id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setEnabled(enabled);
        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserLookupResponse> searchUsers(String query, Pageable pageable) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < 2) {
            return PageResponse.of(
                    List.of(),
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    0,
                    0,
                    "username",
                    "ASC");
        }

        int sanitizedPage = Math.max(pageable.getPageNumber(), 0);
        int sanitizedSize = Math.clamp(pageable.getPageSize(), 1, 20);

        Page<User> page = userRepository.searchEnabledByUsernameOrEmail(
            normalizedQuery,
            PageRequest.of(sanitizedPage, sanitizedSize, Sort.by(Sort.Direction.ASC, "username"))
        );
        List<UserLookupResponse> content = page
                .stream()
            .map(user -> UserLookupResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .avatar(user.getAvatar())
                        .build())
                .toList();

        return PageResponse.of(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                content.size(),
                "username",
                "ASC");
    }

    @Override
    public java.util.Map<UUID, String> lookupUsernames(java.util.Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return userRepository.findAllById(ids)
                .stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getUsername));
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
    }
}
