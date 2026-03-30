package com.finance_tracker.backend_server.account.controller;

import com.finance_tracker.backend_server.account.dto.response.AdminAccountResponse;
import com.finance_tracker.backend_server.account.dto.response.PagedAccountResponse;
import com.finance_tracker.backend_server.account.service.AdminAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing accounts in the admin context.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/accounts")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin – Accounts", description = "Admin account management APIs")
public class AdminAccountController {

    /**
     * The admin service for managing accounts.
     */
    private final AdminAccountService adminAccountService;

    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @Operation(
            summary = "List all accounts",
            description = "Returns a paginated list of all accounts across all users.")
    @GetMapping
    public ResponseEntity<@NonNull PagedAccountResponse> listAllAccounts(
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(adminAccountService.listAllAccounts(pageable));
    }

    @Operation(
            summary = "Disable account",
            description = "Sets the account's active flag to false. Fails if already inactive.")
    @PatchMapping("/{accountId}/disable")
    public ResponseEntity<@NonNull AdminAccountResponse> disableAccount(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(adminAccountService.disableAccount(accountId));
    }
}
