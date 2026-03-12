package com.krypto.coin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCoinRequest {

    @NotBlank(message = "name is required")
    @Size(min = 2, max = 50, message = "name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "symbol is required")
    @Size(min = 2, max = 12, message = "symbol must be between 2 and 12 characters")
    private String symbol;

    private String image;

    @NotNull(message = "initialSupply is required")
    @Positive(message = "initialSupply must be positive")
    private BigDecimal initialSupply;
}
