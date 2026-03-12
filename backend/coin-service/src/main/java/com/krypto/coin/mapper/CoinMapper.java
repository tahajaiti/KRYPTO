package com.krypto.coin.mapper;

import com.krypto.coin.dto.response.CoinResponse;
import com.krypto.coin.entity.Coin;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CoinMapper {

    CoinResponse toResponse(Coin coin);

    List<CoinResponse> toResponseList(List<Coin> coins);
}
