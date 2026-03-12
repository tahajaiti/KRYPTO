package com.krypto.user.mapper;

import com.krypto.user.dto.response.UserResponse;
import com.krypto.user.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);
}
