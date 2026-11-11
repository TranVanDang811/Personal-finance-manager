package com.pfm.personalfinancemanager.mapper;

import com.pfm.personalfinancemanager.dto.response.TransactionResponse;
import com.pfm.personalfinancemanager.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "accountId", source = "account.id")
    @Mapping(target = "accountName", source = "account.accountName")
    @Mapping(target = "accountType", source = "account.accountType")
    @Mapping(target = "toAccountId", source = "toAccount.id")
    @Mapping(target = "toAccountName", source = "toAccount.accountName")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryIcon", source = "category.icon")
    @Mapping(target = "categoryColor", source = "category.color")
    TransactionResponse toResponse(Transaction transaction);
}

