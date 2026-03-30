package com.krypto.trading.client;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.krypto.common.dto.ApiResponse;
import com.krypto.trading.client.dto.UserResponse;;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/users/{userId}")
    ApiResponse<UserResponse> getUserById(@PathVariable UUID userId);

    @GetMapping("/api/users/internal/lookup")
    ApiResponse<Map<UUID, String>> lookupUsernames(@RequestBody Set<UUID> userIds);
}
