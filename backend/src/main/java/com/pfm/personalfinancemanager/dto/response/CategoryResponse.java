package com.pfm.personalfinancemanager.dto.response;

import com.pfm.personalfinancemanager.enums.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private String id;
    private String userId;
    private String name;
    private CategoryType categoryType;
    private String icon;
    private String color;
    private Boolean isSystem;
    private Instant createdAt;
    private Instant updatedAt;
}

