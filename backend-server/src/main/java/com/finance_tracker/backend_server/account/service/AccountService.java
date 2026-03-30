package com.finance_tracker.backend_server.account.service;

import com.finance_tracker.backend_server.account.dto.response.AccountListResponse;
import com.finance_tracker.backend_server.account.dto.response.AccountResponse;
import com.finance_tracker.backend_server.account.dto.request.CreateAccountRequest;
import com.finance_tracker.backend_server.account.dto.request.ChangeAccountStatusRequest;
import com.finance_tracker.backend_server.account.dto.request.UpdateAccountRequest;

/**
 * Service interface for account operations.
 */
public interface AccountService {

    /**
     * Creates a new account owned by the currently authenticated user.
     */
    AccountResponse createAccount(CreateAccountRequest request);

    /**
     * Updates an account owned by the current user.
     */
    AccountResponse updateAccount(Long accountId, UpdateAccountRequest request);

    /**
     * Updates only the active flag for an account owned by the current user.
     */
    AccountResponse changeAccountStatus(Long accountId, ChangeAccountStatusRequest request);

    /**
     * List active accounts for the current user
     */
    AccountListResponse listActiveAccounts();

    /**
     * Get account details for the current user
     */
    AccountResponse getAccountDetails(Long accountId);

}
