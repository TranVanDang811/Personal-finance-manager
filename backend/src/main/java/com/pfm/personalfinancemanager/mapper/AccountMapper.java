package com.pfm.personalfinancemanager.mapper;

import com.pfm.personalfinancemanager.dto.response.AccountResponse;
import com.pfm.personalfinancemanager.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "userId", source = "user.id")
    AccountResponse toResponse(Account account);
}

