package com.krypto.user.controller;

import com.krypto.common.dto.ApiResponse;
import com.krypto.common.dto.PageResponse;
import com.krypto.common.security.AuthorizationUtils;
import com.krypto.user.dto.request.UpdateProfileRequest;
import com.krypto.user.dto.response.UserLookupResponse;
import com.krypto.user.dto.response.UserResponse;
import com.krypto.user.entity.Role;
import com.krypto.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse response = userService.getCurrentUser(AuthorizationUtils.requireUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<UserLookupResponse>> searchUsers(
            @RequestParam String query,
            @PageableDefault(size = 8, sort = "username", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<UserLookupResponse> response = userService.searchUsers(query, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateProfile(AuthorizationUtils.requireUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/me/tutorial")
    public ResponseEntity<ApiResponse<UserResponse>> completeTutorial() {
        UserResponse response = userService.completeTutorial(AuthorizationUtils.requireUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUsername(@PathVariable String username) {
        UserResponse response = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        AuthorizationUtils.requireRole("ADMIN");
        PageResponse<UserResponse> response = userService.getAllUsers(pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(@PathVariable UUID id,
                                                                @RequestParam Role role) {
        AuthorizationUtils.requireRole("ADMIN");
        UserResponse response = userService.updateRole(id, role);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(@PathVariable UUID id,
                                                                  @RequestParam boolean enabled) {
        AuthorizationUtils.requireRole("ADMIN");
        UserResponse response = userService.updateStatus(id, enabled);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/internal/lookup")
    public ResponseEntity<ApiResponse<java.util.Map<UUID, String>>> lookupUsernames(@RequestBody java.util.Set<UUID> ids) {
        java.util.Map<UUID, String> response = userService.lookupUsernames(ids);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
