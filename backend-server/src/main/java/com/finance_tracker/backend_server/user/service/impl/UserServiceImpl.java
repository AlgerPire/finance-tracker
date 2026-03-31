package com.finance_tracker.backend_server.user.service.impl;

import com.finance_tracker.backend_server.common.util.SecurityContextService;
import com.finance_tracker.backend_server.user.dto.response.UserListResponse;
import com.finance_tracker.backend_server.user.dto.response.UserProfileResponse;
import com.finance_tracker.backend_server.user.dto.response.UserSummaryResponse;
import com.finance_tracker.backend_server.user.entity.enumeration.ERole;
import com.finance_tracker.backend_server.user.mapper.UserMapper;
import com.finance_tracker.backend_server.user.repository.UserRepository;
import com.finance_tracker.backend_server.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link UserService} for user-facing operations.
 */
@Service
public class UserServiceImpl implements UserService {

    private final SecurityContextService securityContextService;
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    public UserServiceImpl(SecurityContextService securityContextService,
                           UserMapper userMapper,
                           UserRepository userRepository) {
        this.securityContextService = securityContextService;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile() {
        return userMapper.toProfileDto(securityContextService.getCurrentUser());
    }

    @Override
    @Transactional(readOnly = true)
    public UserListResponse listAllUsers() {
        Long currentUserId = securityContextService.getCurrentUser().getId();
        List<UserSummaryResponse> users = userRepository
                .findAllExcludingRoleAndUser(ERole.ROLE_ADMIN, currentUserId)
                .stream()
                .map(userMapper::toSummaryDto)
                .toList();
        return new UserListResponse(users, null);
    }
}
