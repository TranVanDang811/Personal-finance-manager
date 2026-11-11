package com.pfm.personalfinancemanager.mapper;

import com.pfm.personalfinancemanager.dto.response.UserResponse;
import com.pfm.personalfinancemanager.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
