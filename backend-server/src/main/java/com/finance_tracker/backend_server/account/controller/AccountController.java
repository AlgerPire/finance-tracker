package com.finance_tracker.backend_server.account.controller;

import com.finance_tracker.backend_server.account.dto.AccountListResponse;
import com.finance_tracker.backend_server.account.dto.AccountResponse;
import com.finance_tracker.backend_server.account.dto.CreateAccountRequest;
import com.finance_tracker.backend_server.account.dto.ChangeAccountStatusRequest;
import com.finance_tracker.backend_server.account.dto.UpdateAccountRequest;
import com.finance_tracker.backend_server.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/accounts")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Accounts", description = "Financial account APIs")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "Create account", description = "Creates a new account for the authenticated user")
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<@NonNull AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse saved = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Update account", description = "Updates an account owned by the authenticated user")
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{accountId}")
    public ResponseEntity<@NonNull AccountResponse> updateAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(accountId, request));
    }

    @Operation(
            summary = "Change account status",
            description = "Sets active flag only (e.g. disable with {\"active\": false}) for the authenticated user's account")
    @PreAuthorize("hasRole('USER')")
    @PatchMapping("/{accountId}")
    public ResponseEntity<@NonNull AccountResponse> changeAccountStatus(
            @PathVariable Long accountId,
            @Valid @RequestBody ChangeAccountStatusRequest request) {
        return ResponseEntity.ok(accountService.changeAccountStatus(accountId, request));
    }

    @Operation(
            summary = "List active accounts",
            description = "Returns all active accounts for the authenticated user; when none exist, includes a friendly message")
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public ResponseEntity<@NonNull AccountListResponse> listActiveAccounts() {
        return ResponseEntity.ok(accountService.listActiveAccounts());
    }

    @Operation(summary = "Get account details", description = "Returns full details for one account owned by the authenticated user")
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{accountId}")
    public ResponseEntity<@NonNull AccountResponse> getAccountDetails(@PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getAccountDetails(accountId));
    }


}
