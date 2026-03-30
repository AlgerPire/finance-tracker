package com.finance_tracker.backend_server.transaction.controller;

import com.finance_tracker.backend_server.auth.controller.AuthController;
import com.finance_tracker.backend_server.transaction.dto.request.CreateTransactionRequest;
import com.finance_tracker.backend_server.transaction.dto.response.PagedTransactionsResponse;
import com.finance_tracker.backend_server.transaction.dto.response.TransactionResponse;
import com.finance_tracker.backend_server.transaction.dto.request.TransferRequest;
import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;
import com.finance_tracker.backend_server.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller for managing user transactions.
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/transactions")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
@Tag(name = "Transactions", description = "Account transaction APIs")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    /**
     * The transaction service.
     */
    private final TransactionService transactionService;

    /**
     * Constructor.
     */
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Creates a new transaction.
     */
    @Operation(summary = "Create transaction", description = "Deposit, withdrawal, or transfer for the authenticated user's accounts")
    @PostMapping
    public ResponseEntity<@NonNull TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        logger.info("Transaction request received for account with id: {}", request.accountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.createTransaction(request));
    }

    /**
     * List transactions for the authenticated user.
     */
    @Operation(
            summary = "List my transactions",
            description = "Paginated transactions for the authenticated user. "
                    + "Query: page, size, sort (e.g. sort=transactionAt,desc&sort=id,desc).")
    @GetMapping
    public ResponseEntity<@NonNull PagedTransactionsResponse> listMyTransactions(
            @Parameter(description = "Filter by transaction type; omit for all types")
            @RequestParam(required = false)
            TransactionType type,
            @Parameter(description = "Start of day (UTC), inclusive; ISO date e.g. 2026-03-29")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @Parameter(description = "End of day (UTC), inclusive; ISO date e.g. 2026-03-29")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @ParameterObject
            @PageableDefault(size = 20, sort = {"transactionAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        logger.info("List all transactions");
        return ResponseEntity.ok(transactionService.listTransactionsForCurrentUser(pageable, type, from, to));
    }

    @Operation(
            summary = "Transfer between accounts using account identification code",
            description = "Transfers funds from an account you own to any active account "
                    + "identified by accountIdentification. Accounts must share the same currency.")
    @PostMapping("/transfer-using-account-code")
    public ResponseEntity<@NonNull TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request) {
        logger.info("Request to transfer funds from account with account identification");
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transferUsingAccountIdentification(request));
    }

}