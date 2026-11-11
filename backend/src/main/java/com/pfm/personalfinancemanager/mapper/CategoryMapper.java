package com.pfm.personalfinancemanager.mapper;

import com.pfm.personalfinancemanager.dto.response.CategoryResponse;
import com.pfm.personalfinancemanager.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "userId", source = "user.id")
    CategoryResponse toResponse(Category category);
}

