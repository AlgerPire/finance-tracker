package com.finance_tracker.backend_server.account.service;

import com.finance_tracker.backend_server.account.dto.response.AdminAccountResponse;
import com.finance_tracker.backend_server.account.dto.response.PagedAccountResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for admin account operations.
 */
public interface AdminAccountService {
    PagedAccountResponse listAllAccounts(Pageable pageable);

    AdminAccountResponse disableAccount(Long accountId);
}
