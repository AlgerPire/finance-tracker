package com.finance_tracker.backend_server.user.mapper;

import com.finance_tracker.backend_server.common.mapper.BaseEntityMapper;
import com.finance_tracker.backend_server.user.dto.response.AdminUserResponse;
import com.finance_tracker.backend_server.user.entity.Role;
import com.finance_tracker.backend_server.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper extends BaseEntityMapper<AdminUserResponse, User> {

    @Override
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStrings")
    AdminUserResponse toDto(User user);

    @Override
    default User toEntity(AdminUserResponse dto) {
        throw new UnsupportedOperationException("Admin response cannot be mapped back to User");
    }

    @Named("rolesToStrings")
    static Set<String> rolesToStrings(Set<Role> roles) {
        return roles.stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());
    }
}