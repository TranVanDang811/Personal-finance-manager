package com.pfm.personalfinancemanager.mapper;

import com.pfm.personalfinancemanager.dto.response.NotificationResponse;
import com.pfm.personalfinancemanager.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "userId", source = "user.id")
    NotificationResponse toResponse(Notification notification);
}

