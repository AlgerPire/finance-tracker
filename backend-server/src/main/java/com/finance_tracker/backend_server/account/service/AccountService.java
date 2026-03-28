package com.finance_tracker.backend_server.account.service;

import com.finance_tracker.backend_server.account.dto.AccountListResponse;
import com.finance_tracker.backend_server.account.dto.AccountResponse;
import com.finance_tracker.backend_server.account.dto.CreateAccountRequest;
import com.finance_tracker.backend_server.account.dto.ChangeAccountStatusRequest;
import com.finance_tracker.backend_server.account.dto.UpdateAccountRequest;

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
