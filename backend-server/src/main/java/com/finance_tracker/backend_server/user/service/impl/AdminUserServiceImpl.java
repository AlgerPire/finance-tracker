package com.finance_tracker.backend_server.user.service.impl;

import com.finance_tracker.backend_server.user.dto.response.AdminUserResponse;
import com.finance_tracker.backend_server.user.dto.response.PagedUserResponse;
import com.finance_tracker.backend_server.user.entity.User;
import com.finance_tracker.backend_server.user.entity.enumeration.ERole;
import com.finance_tracker.backend_server.user.mapper.UserMapper;
import com.finance_tracker.backend_server.user.repository.UserRepository;
import com.finance_tracker.backend_server.user.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Order.asc("username"));

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Autowired
    public AdminUserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedUserResponse listAllUsers(Pageable pageable) {
        int size = Math.clamp(pageable.getPageSize(), 1, MAX_PAGE_SIZE);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT;
        Pageable effective = PageRequest.of(pageable.getPageNumber(), size, sort);
        Page<User> page = userRepository.findAllWithRoles(ERole.ROLE_USER, effective);
        List<AdminUserResponse> content =
                page.getContent().stream().map(userMapper::toDto).toList();
        return new PagedUserResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
