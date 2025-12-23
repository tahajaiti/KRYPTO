package com.krypto.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class BaseDTO implements Serializable {

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
