package com.example.medy.core.security.internal.mapper;

import com.example.medy.core.security.internal.dto.RegisterStaffRequestDTO;
import com.example.medy.core.security.internal.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    User toEntity(RegisterStaffRequestDTO request);
}
