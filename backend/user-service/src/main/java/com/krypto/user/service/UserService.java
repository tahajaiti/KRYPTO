package com.krypto.user.service;

import com.krypto.common.dto.PageResponse;
import com.krypto.user.dto.request.UpdateProfileRequest;
import com.krypto.user.dto.response.UserLookupResponse;
import com.krypto.user.dto.response.UserResponse;
import com.krypto.user.entity.Role;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser(String username);

    UserResponse getUserById(UUID id);

    UserResponse getUserByUsername(String username);

    PageResponse<UserResponse> getAllUsers(Pageable pageable);

    UserResponse updateProfile(String username, UpdateProfileRequest request);

    UserResponse completeTutorial(String username);

    UserResponse updateRole(UUID id, Role role);

    UserResponse updateStatus(UUID id, boolean enabled);

    PageResponse<UserLookupResponse> searchUsers(String query, Pageable pageable);

    java.util.Map<UUID, String> lookupUsernames(java.util.Set<UUID> ids);
}
