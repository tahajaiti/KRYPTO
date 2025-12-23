package com.krypto.test.controller;

import com.krypto.common.dto.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    @GetMapping("/ping")
    public Response<String> ping() {
        return Response.success("HELLO", "Service is alive");
    }

    @GetMapping("/db")
    public Response<String> testDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            return Response.success("Connected: " + conn.getMetaData().getDatabaseProductName(), "Database OK");
        } catch (SQLException e) {
            return Response.error(500, "Database Failed", e.getMessage());
        }
    }

    @GetMapping("/redis")
    public Response<String> testRedis() {
        try {
            redisTemplate.opsForValue().set("test_key", "Krypto is working");
            String value = redisTemplate.opsForValue().get("test_key");
            return Response.success(value, "Redis OK");
        } catch (Exception e) {
            return Response.error(500, "Redis Failed", e.getMessage());
        }
    }
}