package com.finance_tracker.backend_server.transaction.controller;

import com.finance_tracker.backend_server.transaction.dto.response.PagedTransactionsResponse;
import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;
import com.finance_tracker.backend_server.transaction.service.AdminTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller for managing transactions in the admin context.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/transactions")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin – Transactions", description = "Admin transaction management APIs")
public class AdminTransactionController {

    private static final Logger logger = LoggerFactory.getLogger(AdminTransactionController.class);

    /**
     * The admin transaction service.
     */
    private final AdminTransactionService adminTransactionService;

    /**
     * Constructor.
     */
    public AdminTransactionController(AdminTransactionService adminTransactionService) {
        this.adminTransactionService = adminTransactionService;
    }

    @Operation(
            summary = "List all transactions",
            description = "Paginated view of every transaction on the platform. "
                    + "Optional filters: type (DEPOSIT, WITHDRAWAL, TRANSFER), "
                    + "userId (initiator), from/to date (UTC calendar day, e.g. 2026-03-29).")
    @GetMapping
    public ResponseEntity<@NonNull PagedTransactionsResponse> listAllTransactions(
            @Parameter(description = "Filter by transaction type; omit for all types")
            @RequestParam(required = false)
            TransactionType type,
            @Parameter(description = "Filter by initiating user ID; omit for all users")
            @RequestParam(required = false)
            Long userId,
            @Parameter(description = "Inclusive start date (UTC), ISO date e.g. 2026-03-29")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @Parameter(description = "Inclusive end date (UTC), ISO date e.g. 2026-03-29")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @ParameterObject
            @PageableDefault(size = 20, sort = {"transactionAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        logger.info("Listing all transactions with pageable: {}", pageable);
        return ResponseEntity.ok(
                adminTransactionService.listAllTransactions(pageable, type, userId, from, to));
    }
}