package com.finance_tracker.backend_server.user.service;

import com.finance_tracker.backend_server.user.dto.response.PagedUserResponse;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    PagedUserResponse listAllUsers(Pageable pageable);
}
