package com.krypto.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Response<T> {
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private int status;
    private String message;
    private T data;
    private String error;

    public static <T> Response<T> success(T data, String message) {
        return Response.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> Response<T> error(int status, String message, String errorDetails) {
        return Response.<T>builder()
                .status(status)
                .message(message)
                .error(errorDetails)
                .build();
    }

}
