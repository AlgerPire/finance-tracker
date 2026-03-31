package com.finance_tracker.backend_server.account.controller;

import com.finance_tracker.backend_server.account.dto.response.AccountListResponse;
import com.finance_tracker.backend_server.account.dto.response.AccountResponse;
import com.finance_tracker.backend_server.account.dto.response.AccountSummaryListResponse;
import com.finance_tracker.backend_server.account.dto.request.CreateAccountRequest;
import com.finance_tracker.backend_server.account.dto.request.ChangeAccountStatusRequest;
import com.finance_tracker.backend_server.account.dto.request.UpdateAccountRequest;
import com.finance_tracker.backend_server.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing user accounts.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/accounts")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
@Tag(name = "Accounts", description = "Financial account APIs for user")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    /**
     * The account service for managing accounts.
     */
    private final AccountService accountService;

    /**
     * Constructor.
     */
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "Create account", description = "Creates a new account for the authenticated user")
    @PostMapping
    public ResponseEntity<@NonNull AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        logger.debug("Create account request received with account type: {}", request.accountType());
        AccountResponse saved = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Update account", description = "Updates an account owned by the authenticated user")
    @PutMapping("/{accountId}")
    public ResponseEntity<@NonNull AccountResponse> updateAccount(@PathVariable Long accountId, @Valid @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(accountId, request));
    }

    @Operation(summary = "Change account status", description = "Sets active flag only (e.g. disable with {\"active\": false}) for the authenticated user's account")
    @PatchMapping("/{accountId}")
    public ResponseEntity<@NonNull AccountResponse> changeAccountStatus(@PathVariable Long accountId, @Valid @RequestBody ChangeAccountStatusRequest request) {
        return ResponseEntity.ok(accountService.changeAccountStatus(accountId, request));
    }

    @Operation(summary = "List active accounts", description = "Returns all active accounts for the authenticated user; when none exist, includes a friendly message")
    @GetMapping
    public ResponseEntity<@NonNull AccountListResponse> listActiveAccounts() {
        return ResponseEntity.ok(accountService.listActiveAccounts());
    }

    @Operation(summary = "Get account details", description = "Returns full details for one account owned by the authenticated user")
    @GetMapping("/{accountId}")
    public ResponseEntity<@NonNull AccountResponse> getAccountDetails(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getAccountDetails(accountId));
    }

    @Operation(summary = "List accounts by user", description = "Returns all accounts (active and inactive) belonging to the specified user, without balance")
    @GetMapping("/user/{userId}")
    public ResponseEntity<@NonNull AccountSummaryListResponse> listAccountsByUserId(@PathVariable Long userId) {
        logger.debug("Listing all accounts for userId: {}", userId);
        return ResponseEntity.ok(accountService.listAccountsByUserId(userId));
    }
}
