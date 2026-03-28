package com.finance_tracker.backend_server.transaction.controller;

import com.finance_tracker.backend_server.transaction.dto.CreateTransactionRequest;
import com.finance_tracker.backend_server.transaction.dto.PagedTransactionsResponse;
import com.finance_tracker.backend_server.transaction.dto.TransactionResponse;
import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;
import com.finance_tracker.backend_server.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/transactions")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Transactions", description = "Account transaction APIs")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "Create transaction", description = "Deposit, withdrawal, or transfer for the authenticated user's accounts")
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<@NonNull TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.createTransaction(request));
    }

    @Operation(
            summary = "List my transactions",
            description = "Paginated transactions for the authenticated user. "
                    + "Query: page, size, sort (e.g. sort=transactionAt,desc&sort=id,desc).")
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public ResponseEntity<@NonNull PagedTransactionsResponse> listMyTransactions(
            @Parameter(description = "Filter by transaction type; omit for all types")
            @RequestParam(required = false)
            TransactionType type,
            @ParameterObject
            @PageableDefault(size = 20, sort = {"transactionAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.listTransactionsForCurrentUser(pageable, type));
    }

}